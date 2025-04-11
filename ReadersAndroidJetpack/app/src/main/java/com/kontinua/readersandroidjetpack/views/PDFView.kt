package com.kontinua.readersandroidjetpack.views

import android.util.Log
import androidx.compose.runtime.* // Import compose runtime functions
import androidx.compose.runtime.saveable.rememberSaveable // For remembering across process death if needed
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import com.kontinua.readersandroidjetpack.serialization.Workbook // Ensure Workbook is imported
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job // Import Job
import kotlinx.coroutines.delay // Import delay
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext

// THIS IS THE VERSION TO USE WITH THE FIXED APIMANAGER
@Composable
fun PDFViewer(modifier: Modifier = Modifier, navbarManager: NavbarManager) {
    val context = LocalContext.current
    val collectionViewModel: CollectionViewModel = viewModel()
    val currentWorkbook by collectionViewModel.workbookState.collectAsState()
    val scope = rememberCoroutineScope()

    // --- State within PDFViewer ---
    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }
    // Store the File object explicitly requested for loading by the Effect
    var requestedFileToLoad by remember { mutableStateOf<File?>(null) }
    // Track the path of the file *currently successfully displayed* in the PDFView
    var pathActuallyLoaded by rememberSaveable { mutableStateOf<String?>(null) }
    // Track if the view is considered ready (load completed for the correct file)
    var isViewReady by rememberSaveable { mutableStateOf(false) }
    // Remember the target page requested by the manager
    val targetPage = navbarManager.currentPage
    // Flag to ignore spurious onPageChange events after programmatic jumps
    var isInternalJumpInProgress by remember { mutableStateOf(false) }
    var jumpDebounceJob by remember { mutableStateOf<Job?>(null) }
    // --- End State ---

    LaunchedEffect(collectionViewModel) {
        navbarManager.setCollection(collectionViewModel)
    }

    // --- Effect 1: Prepare for Workbook Change ---
    LaunchedEffect(currentWorkbook) {
        val workbook = currentWorkbook
        val currentPathInView = pathActuallyLoaded // Path currently displayed

        if (workbook != null) {
            Log.d("PDFViewer", "[Effect Workbook Prep] Workbook changed to ID: ${workbook.id}. Fetching file path...")
            val newFile = withContext(Dispatchers.IO) {
                APIManager.getPDFFromWorkbook(context, workbook) // Fetches or gets cached file with unique name now
            }

            if (newFile != null) {
                // ***Compare paths*** - this will now be different for different workbooks
                if (currentPathInView != newFile.absolutePath) {
                    Log.i("PDFViewer", "[Effect Workbook Prep] New file path detected: ${newFile.absolutePath}. Requesting load.")
                    navbarManager.resetPages()
                    isViewReady = false
                    pathActuallyLoaded = null // Clear the currently loaded path marker
                    jumpDebounceJob?.cancel()
                    isInternalJumpInProgress = false
                    requestedFileToLoad = newFile // Trigger the update block
                } else {
                    Log.d("PDFViewer", "[Effect Workbook Prep] Workbook ID (${workbook.id}) changed, but PDF path ${newFile.absolutePath} is same as loaded. No new load requested.")
                    if (!isViewReady && pdfViewInstance?.pageCount ?: 0 > 0) { // Mark ready if loaded but flag was false
                        isViewReady = true
                    }
                    requestedFileToLoad = newFile // Ensure file is set if view recreates
                }
            } else {
                Log.e("PDFViewer", "[Effect Workbook Prep] Failed to get PDF file for workbook: ${workbook.id}")
                if (pathActuallyLoaded != null) {
                    navbarManager.resetPages()
                    isViewReady = false
                    pathActuallyLoaded = null
                    requestedFileToLoad = null
                }
            }
        } else {
            Log.i("PDFViewer", "[Effect Workbook Prep] Workbook is null. Clearing PDF state.")
            if (pathActuallyLoaded != null) {
                navbarManager.resetPages()
                isViewReady = false
                pathActuallyLoaded = null
                requestedFileToLoad = null
            }
        }
    }

    // --- Effect 2: React to Page Changes from NavbarManager ---
    LaunchedEffect(targetPage, isViewReady, pdfViewInstance) {
        val view = pdfViewInstance
        if (view != null && isViewReady && view.currentPage != targetPage) {
            if (!isInternalJumpInProgress) {
                Log.i("PDFViewer", "[Effect Page] Jumping view from ${view.currentPage} to $targetPage. Setting jump flag.")
                isInternalJumpInProgress = true
                view.jumpTo(targetPage, false) // Use non-animated jump for stability

                jumpDebounceJob?.cancel()
                jumpDebounceJob = scope.launch {
                    delay(150L) // Short delay for non-animated jump flag reset
                    isInternalJumpInProgress = false
                    Log.d("PDFViewer", "[Effect Page] Jump flag cleared after delay.")
                }
            } else {
                Log.d("PDFViewer", "[Effect Page] Internal jump already in progress. Ignoring request to jump to $targetPage.")
            }
        } else if (view != null && isViewReady && view.currentPage == targetPage) {
            if (isInternalJumpInProgress) {
                jumpDebounceJob?.cancel()
                isInternalJumpInProgress = false
                Log.d("PDFViewer", "[Effect Page] View reached target page $targetPage during jump. Clearing flag.")
            }
        }
    }


    // --- AndroidView ---
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Log.d("PDFViewer", "AndroidView Factory: Creating new PDFView instance.")
            PDFView(ctx, null).also { pdfViewInstance = it }
        },
        update = { pdfView ->
            val fileToLoad = requestedFileToLoad // Capture stable ref
            if (fileToLoad != null && pathActuallyLoaded != fileToLoad.absolutePath) {
                Log.i("PDFViewer", "[Update] Loading requested file: ${fileToLoad.name}. Currently loaded: $pathActuallyLoaded")
                isViewReady = false
                pdfView.recycle()

                pdfView.fromFile(fileToLoad)
                    .enableSwipe(true)
                    .swipeHorizontal(true)
                    .pageSnap(true)
                    .pageFling(true)
                    .enableDoubletap(true)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .defaultPage(0)
                    .onLoad { nbPages ->
                        val actualInitialPage = pdfView.currentPage
                        if (fileToLoad.absolutePath == requestedFileToLoad?.absolutePath) {
                            Log.i(
                                "PDFViewer",
                                "[onLoad] Complete: ${fileToLoad.name}. Pages: $nbPages. Initial Page: $actualInitialPage."
                            )
                            pathActuallyLoaded = fileToLoad.absolutePath // Mark this path as loaded
                            isViewReady = true
                            navbarManager.updatePageInfo(actualInitialPage, nbPages)

                            if (actualInitialPage != 0) {
                                Log.w(
                                    "PDFViewer",
                                    "[onLoad] View initial page ($actualInitialPage) is not 0. Syncing."
                                )
                                pdfView.jumpTo(0, false)
                                navbarManager.updatePageInfo(pdfView.currentPage, nbPages)
                            }
                        } else {
                            Log.w(
                                "PDFViewer",
                                "[onLoad] Load completed for ${fileToLoad.name}, but newer file (${requestedFileToLoad?.name}) was requested. Ignoring."
                            )
                        }
                    }
                    .onPageChange { page, pageCount ->
                        val currentPath = pathActuallyLoaded
                        if (!isInternalJumpInProgress && isViewReady && currentPath == requestedFileToLoad?.absolutePath) {
                            Log.d(
                                "PDFViewer",
                                "[onPageChange] Page $page / $pageCount. Updating manager."
                            )
                            navbarManager.updatePageInfo(page, pageCount)
                        } else if (isInternalJumpInProgress) {
                            Log.d(
                                "PDFViewer",
                                "[onPageChange] Page $page / $pageCount. Ignoring update due to internal jump flag."
                            )
                        } else {
                            Log.d(
                                "PDFViewer",
                                "[onPageChange] Page $page / $pageCount. Ignoring update (view not ready or path mismatch)."
                            )
                        }
                    }
                    .onError { t ->
                        if (pathActuallyLoaded != fileToLoad.absolutePath && requestedFileToLoad?.absolutePath == fileToLoad.absolutePath) {
                            pathActuallyLoaded = null
                            isViewReady = false
                            requestedFileToLoad = null
                            navbarManager.resetPages()
                        }
                    }
                    .load()
            }
            else if (fileToLoad == null && pathActuallyLoaded != null) {
                pdfView.recycle()
                isViewReady = false
                pathActuallyLoaded = null
            }
        },
        onRelease = { pdfView ->
            pdfView.recycle()
            pdfViewInstance = null
            isViewReady = false
            pathActuallyLoaded = null
            jumpDebounceJob?.cancel()
            isInternalJumpInProgress = false
        }
    )
}
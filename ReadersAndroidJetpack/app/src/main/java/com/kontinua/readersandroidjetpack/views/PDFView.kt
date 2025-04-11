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
import kotlinx.coroutines.withContext

@Composable
fun PDFViewer(modifier: Modifier = Modifier, navbarManager: NavbarManager) {
    val context = LocalContext.current
    val collectionViewModel: CollectionViewModel = viewModel()
    // Observe the workbook object directly from the ViewModel's state flow
    val currentWorkbook by collectionViewModel.workbookState.collectAsState()

    // --- State within PDFViewer ---
    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }
    // Store the File object *requested* for loading
    var requestedFile by remember { mutableStateOf<File?>(null) }
    // Track the ID of the workbook *currently successfully loaded*
    var loadedWorkbookId by rememberSaveable { mutableStateOf<Int?>(null) }
    // Track if the PDFView is considered ready (load completed)
    var isViewReady by rememberSaveable { mutableStateOf(false) }
    // Keep track of the target page requested by the manager
    val targetPage = navbarManager.currentPage // Read the reactive state from NavbarManager
    // --- End State ---

    // Ensure NavbarManager has the ViewModel reference if needed elsewhere
    LaunchedEffect(collectionViewModel) {
        navbarManager.setCollection(collectionViewModel)
    }


    // --- Effect 1: Fetch PDF File when Workbook Changes ---
    // This effect runs when 'currentWorkbook' (from ViewModel state) changes.
    LaunchedEffect(currentWorkbook) {
        val workbook = currentWorkbook // Capture stable reference
        if (workbook != null) {
            // Only proceed if the workbook ID is different from the one already loaded.
            if (loadedWorkbookId != workbook.id) {
                Log.i("PDFViewer", "[Effect Workbook] Workbook changed to ID: ${workbook.id}. Current loaded ID: $loadedWorkbookId. Fetching PDF...")
                isViewReady = false // Mark as not ready while fetching/loading new workbook
                val file = withContext(Dispatchers.IO) {
                    APIManager.getPDFFromWorkbook(context, workbook)
                }
                if (file != null) {
                    Log.i("PDFViewer", "[Effect Workbook] Fetched PDF: ${file.absolutePath}. Requesting load.")
                    // Reset NavbarManager page state because the content (workbook) is changing.
                    navbarManager.resetPages() // <--- CRUCIAL FOR WORKBOOK CHANGE
                    requestedFile = file       // Set the new file to be loaded by AndroidView's update block
                } else {
                    Log.e("PDFViewer", "[Effect Workbook] Failed to get PDF for workbook ID: ${workbook.id}")
                    requestedFile = null // Clear request on failure
                    loadedWorkbookId = null // Clear loaded marker
                    navbarManager.resetPages() // Also reset if fetch fails
                }
            } else {
                Log.d("PDFViewer", "[Effect Workbook] Workbook state updated, but ID (${workbook.id}) is the same as loaded. No full reset/refetch.")
                // Optional: If the file path could change even for the same ID, you might re-fetch here,
                // but typically the ID change is the main signal.
                // Ensure requestedFile is still set in case the view needs recreation.
                if (requestedFile == null) {
                    val file = withContext(Dispatchers.IO) { APIManager.getPDFFromWorkbook(context, workbook) }
                    requestedFile = file
                }
            }
        } else {
            // Workbook became null in the ViewModel state
            Log.i("PDFViewer", "[Effect Workbook] Workbook is null. Clearing PDF request.")
            requestedFile = null
            isViewReady = false
            loadedWorkbookId = null
            navbarManager.resetPages() // Reset state when no workbook is selected
        }
    }

    // --- Effect 2: React to Page Changes from NavbarManager ---
    // This effect runs when 'targetPage' (read from navbarManager.currentPage) changes,
    // or when the view becomes ready, or when the pdfViewInstance itself changes.
    LaunchedEffect(targetPage, isViewReady, pdfViewInstance) {
        val view = pdfViewInstance
        if (view != null && isViewReady && view.currentPage != targetPage) {
            Log.i("PDFViewer", "[Effect Page] Jumping view from ${view.currentPage} to $targetPage (requested by manager: ${navbarManager.currentPage})")
            // --- CHANGE HERE ---
            view.jumpTo(targetPage, false) // Jump WITHOUT animation
            // --- END CHANGE ---
        } else if (view != null && isViewReady && view.currentPage == targetPage) {
            Log.d("PDFViewer", "[Effect Page] View is ready and already on target page $targetPage.")
        } else if (view != null && !isViewReady) {
            Log.d("PDFViewer", "[Effect Page] View exists but is not ready (isViewReady=$isViewReady). Cannot jump yet.")
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
            val fileToLoad = requestedFile     // Capture stable reference for this update pass
            val workbookIdToLoad = currentWorkbook?.id // Capture stable ID for this update pass

            // --- Condition to Load PDF ---
            // Load if a file is requested AND it corresponds to a workbook different from the currently loaded one.
            // This prevents reloading the same workbook repeatedly during recompositions.
            if (fileToLoad != null && workbookIdToLoad != null && loadedWorkbookId != workbookIdToLoad) {
                Log.i("PDFViewer", "[Update] Loading new workbook PDF. ID: $workbookIdToLoad, File: ${fileToLoad.name}. Target Page: $targetPage")
                isViewReady = false // Mark loading state
                pdfView.recycle()   // Clean previous state before loading new file

                pdfView.fromFile(fileToLoad)
                    .enableSwipe(true)
                    .swipeHorizontal(true)
                    .pageSnap(true)
                    .pageFling(true)
                    .enableDoubletap(true)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    // Use targetPage. When loading a new workbook, targetPage should be 0
                    // because Effect 1 called navbarManager.resetPages().
                    .defaultPage(targetPage)
                    .onLoad(object : OnLoadCompleteListener {
                        override fun loadComplete(nbPages: Int) {
                            val actualInitialPage = pdfView.currentPage
                            Log.i("PDFViewer", "[onLoad] Complete: Workbook ID $workbookIdToLoad. Pages: $nbPages. View Initial Page: $actualInitialPage. Target: $targetPage")
                            // Check if the loaded workbook ID still matches the intended one, in case of race conditions
                            if (workbookIdToLoad == currentWorkbook?.id) {
                                loadedWorkbookId = workbookIdToLoad // Mark this workbook ID as loaded *only if still current*
                                isViewReady = true                 // Mark view ready
                                navbarManager.updatePageInfo(actualInitialPage, nbPages) // Update manager state

                                // Sync page if necessary (e.g., defaultPage didn't work perfectly)
                                if (actualInitialPage != targetPage) {
                                    Log.w("PDFViewer", "[onLoad] View page ($actualInitialPage) differs from target ($targetPage). Syncing.")
                                    pdfView.jumpTo(targetPage, false) // Use non-animated jump for initial sync
                                    // Update manager again if jump occurred and changed the page
                                    if(pdfView.currentPage != actualInitialPage) {
                                        navbarManager.updatePageInfo(pdfView.currentPage, nbPages)
                                    }
                                }
                            } else {
                                Log.w("PDFViewer", "[onLoad] Load completed for $workbookIdToLoad, but current workbook is now ${currentWorkbook?.id}. Ignoring load result.")
                                // Don't set isViewReady or loadedWorkbookId if the workbook changed during load.
                                // The LaunchedEffect(currentWorkbook) should trigger a new load.
                            }
                        }
                    })
                    .onPageChange(object : OnPageChangeListener {
                        override fun onPageChanged(page: Int, pageCount: Int) {
                            // Only update manager if the view is ready and for the currently loaded workbook
                            if (isViewReady && loadedWorkbookId == currentWorkbook?.id) {
                                Log.d("PDFViewer", "[onPageChange] Swipe detected. Page $page / $pageCount")
                                navbarManager.updatePageInfo(page, pageCount) // Update manager on swipe
                            }
                        }
                    })
                    .onError { t ->
                        Log.e("PDFViewer", "[onError] Failed loading workbook $workbookIdToLoad", t)
                        // Reset state if the failed load was for the *currently intended* workbook
                        if (workbookIdToLoad == currentWorkbook?.id) {
                            isViewReady = false
                            loadedWorkbookId = null // Clear loaded marker on error
                            navbarManager.resetPages()
                        }
                    }
                    .load() // Execute the load

            }
            // --- Condition to Clear PDF ---
            // Clear if requested file becomes null AND something was previously loaded (meaning workbook became null)
            else if (fileToLoad == null && loadedWorkbookId != null) {
                Log.i("PDFViewer", "[Update] Requested file is null. Recycling view.")
                pdfView.recycle()
                isViewReady = false
                loadedWorkbookId = null
                // navbarManager should have been reset by Effect 1
            }
            // --- Else: No Load/Recycle Action Needed This Pass ---
            else {
                // This branch means:
                // 1. Initial state before anything is loaded (fileToLoad is null, loadedWorkbookId is null)
                // 2. The requested file/workbook is the same as the one already loaded (fileToLoad!=null, loadedWorkbookId == workbookIdToLoad)
                // In case 2, Effect 2 handles page jumps.
                // Log.v("PDFViewer", "[Update] No load/recycle needed. ReqFile: ${fileToLoad?.name}, LoadedID: $loadedWorkbookId, TargetPage: $targetPage")
            }
        },
        onRelease = { pdfView ->
            Log.i("PDFViewer", "AndroidView onRelease: Recycling PDFView instance.")
            pdfView.recycle()
            pdfViewInstance = null
            // Resetting state here ensures cleanup if the Composable leaves the tree entirely
            isViewReady = false
            loadedWorkbookId = null
        }
    )
}
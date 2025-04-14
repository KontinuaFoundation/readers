package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PDFViewer(modifier: Modifier = Modifier,
              navbarManager: NavbarManager,
              annotationManager: AnnotationManager) {
    val context = LocalContext.current
    val collectionViewModel: CollectionViewModel = viewModel()
    val currentWorkbook by collectionViewModel.workbookState.collectAsState()
    val scope = rememberCoroutineScope()

    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }
    var requestedFileToLoad by remember { mutableStateOf<File?>(null) }
    var pathActuallyLoaded by rememberSaveable { mutableStateOf<String?>(null) }
    var isViewReady by rememberSaveable { mutableStateOf(false) }
    val targetPage = navbarManager.currentPage
    var isInternalJumpInProgress by remember { mutableStateOf(false) }
    var jumpDebounceJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(collectionViewModel) {
        navbarManager.setCollection(collectionViewModel)
    }

    LaunchedEffect(currentWorkbook) {
        val workbook = currentWorkbook
        val currentPathInView = pathActuallyLoaded

        if (workbook != null) {
            val newFile = withContext(Dispatchers.IO) {
                APIManager.getPDFFromWorkbook(context, workbook)
            }

            if (newFile != null) {
                if (currentPathInView != newFile.absolutePath) {
                    navbarManager.resetPages()
                    isViewReady = false
                    pathActuallyLoaded = null
                    jumpDebounceJob?.cancel()
                    isInternalJumpInProgress = false
                    requestedFileToLoad = newFile
                } else {
                    if (!isViewReady && (pdfViewInstance?.pageCount ?: 0) > 0) {
                        isViewReady = true
                    }
                    requestedFileToLoad = newFile
                }
            } else {
                if (pathActuallyLoaded != null) {
                    navbarManager.resetPages()
                    isViewReady = false
                    pathActuallyLoaded = null
                    requestedFileToLoad = null
                }
            }
        } else {
            if (pathActuallyLoaded != null) {
                navbarManager.resetPages()
                isViewReady = false
                pathActuallyLoaded = null
                requestedFileToLoad = null
            }
        }
    }

    LaunchedEffect(targetPage, isViewReady, pdfViewInstance) {
        val view = pdfViewInstance
        if (view != null && isViewReady && view.currentPage != targetPage) {
            if (!isInternalJumpInProgress) {
                isInternalJumpInProgress = true
                view.jumpTo(targetPage, false)

                jumpDebounceJob?.cancel()
                jumpDebounceJob = scope.launch {
                    delay(150L)
                    isInternalJumpInProgress = false
                }
            }
        } else if (view != null && isViewReady && view.currentPage == targetPage) {
            if (isInternalJumpInProgress) {
                jumpDebounceJob?.cancel()
                isInternalJumpInProgress = false
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                PDFView(ctx, null).also { pdfViewInstance = it }
            },
            update = { pdfView ->
                val fileToLoad = requestedFileToLoad
                if (fileToLoad != null && pathActuallyLoaded != fileToLoad.absolutePath) {
                    isViewReady = false
                    pdfView.recycle()

                    pdfView.fromFile(fileToLoad)
                        .enableSwipe(true)
                        .swipeHorizontal(true)
                        .enableDoubletap(true)
                        .defaultPage(navbarManager.pageNumber)
                        .onPageChange { page, _ ->
                            navbarManager.setPage(page)
                        }
                        .pageFling(true)
                        .pageSnap(true)
                        .pageFling(true)
                        .enableDoubletap(true)
                        .pageFitPolicy(FitPolicy.WIDTH)
                        .defaultPage(0)
                        .onLoad { nbPages ->
                            val actualInitialPage = pdfView.currentPage
                            if (fileToLoad.absolutePath == requestedFileToLoad?.absolutePath) {
                                pathActuallyLoaded = fileToLoad.absolutePath
                                isViewReady = true
                                navbarManager.updatePageInfo(actualInitialPage, nbPages)

                                if (actualInitialPage != 0) {
                                    pdfView.jumpTo(0, false)
                                    navbarManager.updatePageInfo(pdfView.currentPage, nbPages)
                                }
                            }
                        }
                        .onPageChange { page, pageCount ->
                            val currentPath = pathActuallyLoaded
                            if (!isInternalJumpInProgress && isViewReady && currentPath == requestedFileToLoad?.absolutePath) {
                                navbarManager.updatePageInfo(page, pageCount)
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
                } else if (fileToLoad == null && pathActuallyLoaded != null) {
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
        if (annotationManager.scribbleEnabled) {
            // Drawing Canvas
            DrawingCanvas(
                workbookId = navbarManager.currentWorkbook,
                page = navbarManager.pageNumber,
                annotationManager = annotationManager
            )
        }
    }
}

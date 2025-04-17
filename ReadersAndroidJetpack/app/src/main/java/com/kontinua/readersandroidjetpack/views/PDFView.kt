package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.barteksc.pdfviewer.PDFView
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import java.io.File

@Composable
fun PDFViewer(modifier: Modifier = Modifier,
              navbarManager: NavbarManager,
              annotationManager: AnnotationManager) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    val scrollOffset = remember { mutableFloatStateOf(0f) }
    val zoomFactor = remember { mutableFloatStateOf(1f) }

    // Anything that needs to access workbooks should receive it from this component!
    // i.e. Do NOT instantiate CollectionViewModel anywhere else, pass it down.
    val collectionViewModel: CollectionViewModel = viewModel()
    val collection by collectionViewModel.collectionState.collectAsState()
    val workbook by collectionViewModel.workbookState.collectAsState()
    var chapterClicked by remember { mutableStateOf(false) }

    navbarManager.setCollection(collectionViewModel)
    chapterClicked = navbarManager.chapterClicked

    LaunchedEffect(workbook, chapterClicked) {
        val file = workbook?.let { APIManager.getPDFFromWorkbook(context, it) }
        navbarManager.setClicked(false)
        if (file != null) {
            pdfFile = file
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> PDFView(ctx, null) },
            update = { pdfView ->
                pdfFile?.let { file ->
                    pdfView.fromFile(file)
                        .enableSwipe(true)
                        .swipeHorizontal(true)
                        .enableDoubletap(true)
                        .defaultPage(navbarManager.pageNumber)
                        .pageFling(true)
                        .pageSnap(true)
                        .onPageChange { page, _ ->
                            navbarManager.setPage(page)
                        }
                        .onPageScroll { page, offset ->
                            scrollOffset.floatValue = offset
                        }
                        .load()
                    pdfView.jumpTo(navbarManager.pageNumber)
                }
            }
        )

        key(navbarManager.currentWorkbook, navbarManager.pageNumber){
            DrawingCanvas(
                workbookId = navbarManager.currentWorkbook,
                page = navbarManager.pageNumber,
                annotationManager = annotationManager,
                offset = scrollOffset.floatValue
            )
        }
    }
    pdfFile = null
}




package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
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

    // Anything that needs to access workbooks should receive it from this component!
    // i.e. Do NOT instantiate CollectionViewModel anywhere else, pass it down.
    val collectionViewModel: CollectionViewModel = viewModel()
    val workbook by collectionViewModel.workbookState.collectAsState()
    var chapterClicked by remember { mutableStateOf(false) }
    val currentZoom = remember { mutableFloatStateOf(1f) }
    val zoomPoint = remember { mutableStateOf(Offset.Zero) }
    val panOffset = remember { mutableStateOf(Offset.Zero) }

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
            factory = { ctx ->
                // adds zoom overlay over pdfview in order to grab where the zoom is occurring at
                val overlay = ZoomOverlay(context)
                val pdfView = PDFView(ctx, null)

                overlay.addView(pdfView)

                overlay.onGestureFocus = { pointF ->
                    zoomPoint.value = Offset(pointF.x, pointF.y)
                }

                overlay.tag = pdfView
                overlay },
            update = { viewGroup ->
                val pdfView = viewGroup.tag as PDFView
                // resets zoom to center when not zoomed
                if (currentZoom.floatValue == 1f){
                    zoomPoint.value = Offset(pdfView.width / 2f, pdfView.height / 2f)
                }
                pdfFile?.let { file ->
                    pdfView.fromFile(file)
                        .enableSwipe(true)
                        .swipeHorizontal(true)
                        .enableDoubletap(true)
                        .defaultPage(navbarManager.pageNumber)
                        .pageFling(true)
                        .pageSnap(true)
                        .onPageChange(object : OnPageChangeListener {
                          override fun onPageChanged(page: Int, pageCount: Int) {
                              navbarManager.setPage(page)
                              navbarManager.setPageCountValue(pageCount)
                          }
                        })
                        .onLoad(object : OnLoadCompleteListener {
                          override fun loadComplete(nbPages: Int) {
                            navbarManager.setPage(pdfView.currentPage)
                          }
                        })
                        .onPageScroll { page, offset ->
                            currentZoom.floatValue = pdfView.zoom
                            panOffset.value = Offset(
                                -pdfView.currentXOffset,
                                -pdfView.currentYOffset
                            )
                        }
                        .load()
                    pdfView.jumpTo(navbarManager.pageNumber)
                }
            }
        )

        DrawingCanvas(
            workbookId = navbarManager.currentWorkbook,
            page = navbarManager.pageNumber,
            annotationManager = annotationManager,
            context = context,
            zoom = currentZoom.floatValue,
            pan = panOffset.value
        )
    }
    pdfFile = null
}




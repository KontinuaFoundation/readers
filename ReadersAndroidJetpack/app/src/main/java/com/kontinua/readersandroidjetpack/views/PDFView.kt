package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.barteksc.pdfviewer.PDFView
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.BookmarkViewModel
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import java.io.File

@Composable
fun PDFViewer(
    modifier: Modifier = Modifier,
    navbarManager: NavbarManager,
    collectionViewModel: CollectionViewModel,
    annotationManager: AnnotationManager,
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var lastLoadedFile by remember { mutableStateOf<File?>(null) }
    val workbook by collectionViewModel.workbookState.collectAsState()
    val collectionViewModel: CollectionViewModel = viewModel()
    val currentZoom = remember { mutableFloatStateOf(1f) }
    val zoomPoint = remember { mutableStateOf(Offset.Zero) }
    val panOffset = remember { mutableStateOf(Offset.Zero) }
    val allBookmarks by bookmarkViewModel.bookmarkLookup.collectAsState()
    val isPageBookmarked = workbook?.id?.let { wbId ->
        allBookmarks[wbId]?.contains(navbarManager.pageNumber)
    } ?: false

    navbarManager.setCollection(collectionViewModel)

    LaunchedEffect(collectionViewModel) {
        navbarManager.setCollection(collectionViewModel)
    }

    LaunchedEffect(workbook) {
        // whenever workbook switches, force reload
        pdfFile = null
        lastLoadedFile = null
    }

    // only fetch new file when workbook changes
    LaunchedEffect(workbook) {
        workbook?.let {
            APIManager.getPDFFromWorkbook(context, it)
        }?.also { pdfFile = it }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // adds zoom overlay over pdfview in order to grab where the zoom is occurring at
                val overlay = ZoomView(context)
                val pdfView = PDFView(ctx, null)

                overlay.addView(pdfView)

                overlay.onGestureFocus = { pointF ->
                    zoomPoint.value = Offset(pointF.x, pointF.y)
                }

                overlay.tag = pdfView
                overlay
            },
            update = { viewGroup ->
                val pdfView = viewGroup.tag as PDFView
                // resets zoom to center when not zoomed
                if (currentZoom.floatValue == 1f) {
                    zoomPoint.value = Offset(pdfView.width / 2f, pdfView.height / 2f)
                }
                pdfFile?.let { file ->
                    if (lastLoadedFile != file) {
                        lastLoadedFile = file
                        pdfView.fromFile(file)
                            .enableSwipe(true)
                            .swipeHorizontal(true)
                            .enableDoubletap(true)
                            .defaultPage(navbarManager.pageNumber)
                            .pageFling(true)
                            .pageSnap(true)
                            .onPageChange { page, pageCount ->
                                navbarManager.setPage(page)
                                navbarManager.setPageCountValue(pageCount)
                            }
                            .onLoad { navbarManager.setPage(pdfView.currentPage) }
                            .onPageScroll { page, offset ->
                                currentZoom.floatValue = pdfView.zoom
                                panOffset.value = Offset(
                                    -pdfView.currentXOffset,
                                    -pdfView.currentYOffset
                                )
                            }
                            .load()
                    }
                    // only jump to the new page if it’s different
                    val target = navbarManager.pageNumber
                    if (pdfView.currentPage != target) {
                        pdfView.jumpTo(target, true)
                    }
                }
            }
        )
        if (pdfFile != null) {
            val workbookId = "Workbook ${collectionViewModel.currentWorkbook.number}"
            DrawingCanvas(
                workbookId = workbookId,
                page = navbarManager.pageNumber,
                annotationManager = annotationManager,
                context = context,
                zoom = currentZoom.floatValue,
                pan = panOffset.value
            )
        }
        IconButton(
            onClick = {
                workbook?.id?.let { wbId ->
                    bookmarkViewModel.toggleBookmark(wbId, navbarManager.pageNumber)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isPageBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isPageBookmarked) "Remove Bookmark" else "Add Bookmark"
            )
        }
    }
}

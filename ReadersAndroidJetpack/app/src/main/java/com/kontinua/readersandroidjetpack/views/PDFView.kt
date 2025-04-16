package com.kontinua.readersandroidjetpack.views
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import java.io.File

@Composable
fun PDFViewer(
    modifier: Modifier = Modifier,
    navbarManager: NavbarManager,
    collectionViewModel: CollectionViewModel
) {
    val context = LocalContext.current
    var targetPdfFile by remember { mutableStateOf<File?>(null) }
    var loadedFilePath by remember { mutableStateOf<String?>(null) }
    val workbook by collectionViewModel.workbookState.collectAsState()

    LaunchedEffect(workbook) {
        val currentWorkbook = workbook
        targetPdfFile = null
        loadedFilePath = null
        val file = currentWorkbook?.let { APIManager.getPDFFromWorkbook(context, it) }
        if (file != null && file.exists()) {
            targetPdfFile = file
        }
    }

    val targetPage = remember(navbarManager.currentChapterIndex) {
        navbarManager.pageNumber
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PDFView(ctx, null)
        },
        update = { pdfView ->
            val currentTargetFile = targetPdfFile
            val currentLoadedPath = loadedFilePath

            if (currentTargetFile != null) {
                if (currentTargetFile.path != currentLoadedPath) {
                    loadedFilePath = null

                    pdfView.fromFile(currentTargetFile)
                        .defaultPage(0)
                        .enableSwipe(true)
                        .swipeHorizontal(true)
                        .enableDoubletap(true)
                        .pageFling(true)
                        .pageSnap(true)
                        .onPageChange { page, pageCount ->
                            navbarManager.setPage(page)
                            navbarManager.setPageCount(pageCount)
                        }
                        .onLoad { nbPages ->
                            val loadedPath = currentTargetFile.path
                            loadedFilePath = loadedPath
                            navbarManager.setPageCount(nbPages)
                            val finalTargetPage = navbarManager.pageNumber.coerceIn(0, nbPages - 1)
                            if (pdfView.currentPage != finalTargetPage) {
                                pdfView.jumpTo(finalTargetPage, false)
                            }
                        }
                        .load()
                }
                else if (
                    pdfView.currentPage != targetPage && pdfView.pageCount > 0) {
                    pdfView.jumpTo(targetPage, false)
                }
            } else {
                if (loadedFilePath != null) {
                    loadedFilePath = null
                }
            }
        }
    )
}
package com.kontinua.readersandroidjetpack.views
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import java.io.File

//TODO: pages are recomposing as they change, making for messy swiping.

@Composable
fun PDFViewer(modifier: Modifier = Modifier, navbarManager: NavbarManager, collectionViewModel: CollectionViewModel) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    val workbook by collectionViewModel.workbookState.collectAsState()
    navbarManager.setCollection(collectionViewModel)

    LaunchedEffect(workbook) {
        val file = workbook?.let { APIManager.getPDFFromWorkbook(context, it) }
        if (file != null) {
            pdfFile = file
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PDFView(ctx, null)
        },
        update = { pdfView ->
            pdfFile?.let { file ->
                pdfView.fromFile(file)
                    .enableSwipe(true)
                    .swipeHorizontal(true)
                    .enableDoubletap(true)
                    .defaultPage(navbarManager.pageNumber)
                    .onPageChange{ page, pageCount ->
                        navbarManager.setPage(page)
                        navbarManager.setPageCountValue(pageCount)
                    }
                    .pageFling(true)
                    .pageSnap(true)
                    .onPageChange { page, pageCount ->
                        navbarManager.setPage(page)
                        navbarManager.setPageCountValue(pageCount)
                    }
                    .onLoad { navbarManager.setPage(pdfView.currentPage) }
                    .load()
                pdfView.jumpTo(navbarManager.pageNumber)
            }
        }
    )
    pdfFile = null
}
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

// TODO: pages are recomposing as they change, making for messy swiping.

@Composable
fun PDFViewer(
    modifier: Modifier = Modifier,
    navbarManager: NavbarManager,
    collectionViewModel: CollectionViewModel
) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    // file currently in the view
    var lastLoadedFile by remember { mutableStateOf<File?>(null) }
    val workbook by collectionViewModel.workbookState.collectAsState()

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

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PDFView(ctx, null)
        },
        update = { pdfView ->
            pdfFile?.let { file ->
                if (lastLoadedFile != file) {
                    lastLoadedFile = file
                    pdfView.fromFile(file)
                        .enableSwipe(true)
                        .swipeHorizontal(true)
                        .enableDoubletap(true)
                        .pageFling(true)
                        .pageSnap(true)
                        .defaultPage(navbarManager.pageNumber)
                        .onPageChange { page, count ->
                            navbarManager.setPage(page)
                            navbarManager.setPageCountValue(count)
                        }
                        .onLoad { navbarManager.setPage(pdfView.currentPage) }
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
}

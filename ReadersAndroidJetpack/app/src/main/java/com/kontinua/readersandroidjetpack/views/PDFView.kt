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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import java.io.File

@Composable
fun PDFViewer(modifier: Modifier = Modifier, navbarManager: NavbarManager, collectionViewModel: CollectionViewModel) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }

    val collection by collectionViewModel.collectionState.collectAsState()
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
                    // Use the correct page change listener method
                    .onPageChange(object : OnPageChangeListener {
                        override fun onPageChanged(page: Int, pageCount: Int) {
                            navbarManager.setPage(page)
                            navbarManager.setPageCountValue(pageCount)
                        }
                    })
                    // Use the correct load complete listener
                    .onLoad(object : OnLoadCompleteListener {
                        override fun loadComplete(nbPages: Int) {
                            navbarManager.setPage(pdfView.currentPage)
                        }
                    })
                    .load()
                pdfView.jumpTo(navbarManager.pageNumber)
            }
        }
    )
    pdfFile = null
}




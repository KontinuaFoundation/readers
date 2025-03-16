package com.kontinua.readersandroidjetpack

import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfViewerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }

    // Anything that needs to access workbooks should receive it from this component!
    // i.e. Do NOT instantiate CollectionViewModel anywhere else, pass it down.
    val collectionViewModel: CollectionViewModel = viewModel()
    val collection by collectionViewModel.collectionState.collectAsState()
    val workbook by collectionViewModel.workbookState.collectAsState()


    LaunchedEffect(workbook) {
        val file = workbook?.let { downloadPdf(context, it.pdf) }
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
                    .defaultPage(0)
                    .pageFling(true)
                    .pageSnap(true)
                    .load()
            }
        }
    )
}

suspend fun downloadPdf(context: android.content.Context, url: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = HttpClient.instance.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e("Download PDF", "Failed: ${response.code}")
                return@withContext null
            }

            response.body?.let { body ->
                val file = File(context.cacheDir, "downloaded.pdf")
                val fos = FileOutputStream(file)
                fos.use { output ->
                    output.write(body.bytes())
                }
                return@withContext file
            }
        } catch (e: Exception) {
            Log.e("Download PDF", "Error downloading PDF: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

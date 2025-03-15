package com.kontinua.readersandroidjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadersAndroidJetpackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PdfViewerScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        pdfUrl = "https://kontinua-foundation-workbook-pdfs.s3.us-east-2.amazonaws.com/workbook-01-en-US-1.0.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASDRANPQQ52CWNAYG%2F20250315%2Fus-east-2%2Fs3%2Faws4_request&X-Amz-Date=20250315T014548Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Security-Token=IQoJb3JpZ2luX2VjELH%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMiJHMEUCIH3bjvNmyXbr8OXui7qP8agBSRFSfnSE6ZlU7PmX2N3%2FAiEA1SwEVS63uSqhGYhIq%2F3nQRZDpvZjKxNfF2DBZUWMuYMqxQUI%2Bv%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAAGgwxNDUwMjMxMzg4NDkiDPKrh4qfYKk6XvIRWyqZBWb8euVTwinaGpdVSz34Hr2LxB0H7XDgnx69U8RIKoMZXk3CD9UbO2gp1Kf4gCWPWJxdVg7BmjbGyXgTMQQnGefYQ0yKYo5QCp%2FQoMO2O5DE26KKgGVKG80jIG5oeOGoGbXVuj9QCruKwvfO2qF1rGUEVvOdA8Vm5rHMJqeeenn15xvF5IDK9iDnI%2FGIHla8PyI2db%2FVGMa2eLWQKO7H5q4B3s7u5qe3jlKD14VnQqy6pEaE89WDb449YsKmYRa1vCKqQIxZU1TT9LLg9ltPFXGqgKHcnMVX9J60LBjC5MB0YM9Si%2BWwZ3bk6CE1WdqWd%2Fn2XEEdFsjOUStrgtS5Quai645v49BNegi3s%2FHMOFltop0zVu3bIsoXIMHNd09g2U6fmqYCi8PvWpUSR4nO0ebGoHuewif46%2BjkvoWJn1xbhs0Txq70w%2FPeIGnKur56VXde0RxdF8QPrfdiv%2Fn7yU%2FJajiRLVPpYQNH5yW%2BAC%2FvN5QLzFYULkoSzRL%2FqCBQ%2FTcNb0o142fnwXPn%2B8iCV3K94sNxpDvFoRvf201hOvPZgAKxFUosUxiEInWOWbGFNgyfycfqE%2BLzHwMwaAoIGIJxLDIeDyVRmCsqMP56nqtCyjYkiAdNfzizdplMD1XLyLeOjuHhFBvLMVLC5mTgpLUxh8tYMPFIyp4hIFodFjHwrw3ly6CCfL04YkHsDulq5QbB714yh9jH4YbwmHZ8YDLNqgGHucbA%2BvMF1W7PZIz8MDpNWKSwf%2FFQBCkb%2B4DsC9rLUUYkRu%2F89N10q%2Be8k30qy9xFePHCzvhGu8FsoDaRdKJNsqQhUwxjFgTbPdKegzuxuQJUANfXQhddfvrvUUf9YeCDzOPXZlXyiRDFUpg7tqWfOZyJqgahMIei074GOrEBtDQQVbh1tD3PysqQpfqY4cRsjrJZkaxbjwEjGs7k%2FO3tCcaCnZrheoKVmYk4bcBZd6nNt7tWkCUXPk8BEejZ3qzPTN9Er1Cxb%2F9Gsc4zFt40i3kExMC16t5uECFFNHlgzasRjPqxDCqto%2BpiplCICDlI8AKWfgGT87Oks9CPnXnaEgytN8oJHtRAF86iwPFHhlPHlMROFB%2F9QI2zMJFQ%2F0I4NRJykcys3fPjICkV9jaz&X-Amz-Signature=99623c47d5c2dd557290c3adb7415acbdfaedd99a3e45cd8b96a64c63fc406f2"
                    )
                }
            }
        }
    }
}

@Composable
fun PdfViewerScreen(modifier: Modifier = Modifier, pdfUrl: String) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(pdfUrl) {
        val file = downloadPdf(context, pdfUrl)
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

/**
 * Downloads the PDF from the given URL and saves it to the app's internal storage.
 */
suspend fun downloadPdf(context: android.content.Context, url: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

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
                Log.d("Download PDF", "Download successful")
                return@withContext file
            }
        } catch (e: Exception) {
            Log.e("Download PDF", "Error downloading PDF: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

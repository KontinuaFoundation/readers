package com.kontinua.readerandroid

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class PdfLoader(
    private val context: Context,
    private val apiService: MainActivity.ApiService,
    private val baseUrl: String,
    private val pdfFileName: String,
    private val onPdfLoaded: (File) -> Unit,
    private val onFailure: (String) -> Unit
) {

    fun loadPdf() {
        val pdfUrl = "pdfs/$pdfFileName"
        Log.d("PdfLoader", "Loading PDF from: $pdfUrl")

        val call = apiService.getPdfData(baseUrl + pdfUrl)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val pdfFile = savePdfToCache(body, pdfFileName)
                                withContext(Dispatchers.Main) {
                                    onPdfLoaded(pdfFile) // Callback to MainActivity
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onFailure("Error saving PDF: ${e.message}")
                                }
                            }
                        }
                    }
                } else {
                    onFailure("Failed to download PDF: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onFailure("Network error: ${t.message}")
            }
        })
    }

    private suspend fun savePdfToCache(body: ResponseBody, fileName: String): File =
        withContext(Dispatchers.IO) {
            val pdfFile = File(context.cacheDir, fileName)
            try {
                val outputStream = FileOutputStream(pdfFile)
                outputStream.use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                pdfFile
            } catch (e: Exception) {
                Log.e("PdfLoader", "Error saving PDF to cache: ${e.message}")
                throw e
            }
        }
}
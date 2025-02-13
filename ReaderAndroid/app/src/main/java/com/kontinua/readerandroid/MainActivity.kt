package com.kontinua.readerandroid

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var loadingTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private val baseUrl = "http://10.0.2.2:8000/"
    private val pdfFileName = "workbook-01.pdf"  // Hardcoded filename

    interface ApiService {
        @GET
        fun getPdfData(@Url url: String): Call<ResponseBody>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.pdfImageView)
        loadingTextView = findViewById(R.id.loadingTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        loadPdfFromUrl()
    }

    private fun loadPdfFromUrl() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val pdfUrl = "pdfs/$pdfFileName" // Create hardcoded endpoint
        Log.d("MainActivity", "Loading PDF from: $pdfUrl")  // Log the URL

        val call = apiService.getPdfData(baseUrl + pdfUrl)

        call.enqueue(object : Callback<ResponseBody> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val pdfFile = savePdfToCache(this@MainActivity, body, pdfFileName)
                            withContext(Dispatchers.Main) {
                                openPdf(pdfFile)
                            }
                        }
                    }
                } else {
                    Log.e("MainActivity", "Failed to download PDF: ${response.code()}")
                    loadingTextView.text = "Failed to load PDF"
                    loadingProgressBar.visibility = View.GONE
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("MainActivity", "Network error: ${t.message}")
                loadingTextView.text = "Network Error"
                loadingProgressBar.visibility = View.GONE
            }
        })
    }

    private suspend fun savePdfToCache(context: MainActivity, body: ResponseBody, fileName: String): File =
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
                Log.e("MainActivity", "Error saving PDF to cache: ${e.message}")
                throw e
            }
        }

    @SuppressLint("SetTextI18n")
    private fun openPdf(pdfFile: File) {
        var descriptor: ParcelFileDescriptor? = null // Local Variable
        try {
            descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)  // Local Variable

            // Safely update mutable state on main thread:
            CoroutineScope(Dispatchers.Main).launch {
                parcelFileDescriptor = descriptor
                pdfRenderer = renderer
                displayPage(0)  // Load the first page
            }

        } catch (e: IOException) {
            Log.e("MainActivity", "Error opening PDF: ${e.message}")
            // Clean up
            descriptor?.close()
            loadingTextView.text = "Error opening PDF"
            loadingProgressBar.visibility = View.GONE

        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening PDF: ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayPage(index: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val renderer = pdfRenderer  //Local Variable

            if (renderer == null) {
                Log.e("MainActivity", "PdfRenderer is null. PDF might not be opened yet.")
                return@launch
            }

            if (index < 0 || index >= renderer.pageCount) {
                Log.e("MainActivity", "Invalid page index: $index")
                return@launch
            }

            try {
                val page = renderer.openPage(index)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                imageView.setImageBitmap(bitmap)
                page.close()

                loadingTextView.visibility = View.GONE  //Hide the loading message
                loadingProgressBar.visibility = View.GONE // Hide the progress bar
                imageView.visibility = View.VISIBLE //Show the PDF
            } catch (e: Exception) {
                Log.e("MainActivity", "Error rendering page: ${e.message}")
                loadingTextView.text = "Error Rendering PDF"
                loadingProgressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error closing PDF resources: ${e.message}")
        }
    }
}
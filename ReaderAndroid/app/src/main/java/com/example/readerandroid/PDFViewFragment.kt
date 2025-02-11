package com.kontinua.readerandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PDFViewFragment : Fragment() {

    private lateinit var imageView: ImageView
    private var fileName: String? = null
    private var currentPage: Int = 0
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    companion object {
        private const val BASE_URL = "http://localhost:8000/"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pdf_view, container, false)
        imageView = view.findViewById(R.id.pdfImageView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load PDF when the view is created
        loadPdfFromUrl()
    }

    fun setFileName(fileName: String?) {
        this.fileName = fileName
        loadPdfFromUrl() // Reload the PDF when the file name changes
    }

    private fun loadPdfFromUrl() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        fileName?.let { fileName ->
            val pdfUrl = "pdfs/$fileName"
            val call = apiService.getPdfData(BASE_URL + pdfUrl)

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val pdfFile = savePdfToCache(requireContext(), body, fileName)
                                withContext(Dispatchers.Main) {
                                    openPdf(pdfFile)
                                    displayPage(currentPage)
                                }
                            }
                        }
                    } else {
                        Log.e("PDFViewFragment", "Failed to download PDF: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("PDFViewFragment", "Network error: ${t.message}")
                }
            })
        }
    }

    private suspend fun savePdfToCache(context: Context, body: ResponseBody, fileName: String): File =
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
                Log.e("PDFViewFragment", "Error saving PDF to cache: ${e.message}")
                throw e // Re-throw the exception to be handled upstream
            }
        }

    private fun openPdf(pdfFile: File) {
        var descriptor: ParcelFileDescriptor? = null // local variable
        try {
            descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)  // local variable

            // Safely update mutable state on main thread:
            CoroutineScope(Dispatchers.Main).launch {
                parcelFileDescriptor = descriptor
                pdfRenderer = renderer

                displayPage(currentPage)
            }

        } catch (e: IOException) {
            Log.e("PDFViewFragment", "Error opening PDF: ${e.message}")
            // Clean up
            descriptor?.close()
        } catch (e: Exception) {
            Log.e("PDFViewFragment", "Error opening PDF: ${e.message}")
        }
    }

    private fun displayPage(index: Int) {
        val renderer = pdfRenderer // Access through local variable (immutable)

        if (renderer == null) {
            Log.e("PDFViewFragment", "PdfRenderer is null. PDF might not be opened yet.")
            return
        }

        if (index < 0 || index >= renderer.pageCount) {
            Log.e("PDFViewFragment", "Invalid page index: $index")
            return
        }

        try {
            val page = renderer.openPage(index)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            imageView.setImageBitmap(bitmap)
            page.close()
            currentPage = index
        } catch (e: Exception) {
            Log.e("PDFViewFragment", "Error rendering page: ${e.message}")
        }
    }

    fun goToNextPage() {
        displayPage(currentPage + 1)
    }

    fun goToPreviousPage() {
        displayPage(currentPage - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        } catch (e: Exception) {
            Log.e("PDFViewFragment", "Error closing PDF resources: ${e.message}")
        }
    }
}
package com.kontinua.readerandroid

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class PdfDisplayHelper(
    private val imageView: ImageView,
    private val loadingTextView: TextView,
    private val onDisplay: (Int) -> Unit,
    private val onDisplayFailed: (String) -> Unit
) {

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    @SuppressLint("SetTextI18n")
    fun openPdf(pdfFile: File) {
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
            CoroutineScope(Dispatchers.Main).launch {
                onDisplayFailed("Error opening PDF")
            }

        } catch (e: Exception) {
            Log.e("PdfDisplayHelper", "Error opening PDF: ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    fun displayPage(index: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val renderer = pdfRenderer ?: run {
                Log.e("PdfDisplayHelper", "PdfRenderer is null. PDF might not be opened yet.")
                return@launch
            }

            if (index < 0 || index >= renderer.pageCount) {
                Log.e("PdfDisplayHelper", "Invalid page index: $index")
                return@launch
            }

            try {
                val page = renderer.openPage(index)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                imageView.setImageBitmap(bitmap)
                page.close()

                onDisplay(index) // Callback to MainActivity
            } catch (e: Exception) {
                Log.e("PdfDisplayHelper", "Error rendering page: ${e.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    onDisplayFailed("Error Rendering PDF")
                }
            }
        }
    }

    fun closePdf() {
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        } catch (e: Exception) {
            Log.e("PdfDisplayHelper", "Error closing PDF resources: ${e.message}")
        }
    }

    fun goToNextPage(currentPageIndex: Int, onNext: (Int) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val renderer = pdfRenderer ?: return@launch
            if (currentPageIndex < renderer.pageCount - 1) {
                onNext(currentPageIndex + 1)
            }
        }
    }

    fun goToPreviousPage(currentPageIndex: Int, onPrevious: (Int) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            if (currentPageIndex > 0) {
                onPrevious(currentPageIndex - 1)
            }
        }
    }
}
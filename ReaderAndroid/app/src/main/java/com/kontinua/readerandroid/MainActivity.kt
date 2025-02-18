package com.kontinua.readerandroid

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
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

class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {

    private lateinit var imageView: ImageView
    private lateinit var loadingTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var toolbar: Toolbar
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var pageNumberTextView: TextView
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPageIndex: Int = 0 // Track the current page index
    private lateinit var gestureDetector: GestureDetectorCompat
    private val baseUrl = "http://10.0.2.2:8000/"
    private val pdfFileName = "workbook-01.pdf"

    interface ApiService {
        @GET
        fun getPdfData(@Url url: String): Call<ResponseBody>
    }
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.pdfImageView)
        loadingTextView = findViewById(R.id.loadingTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
        pageNumberTextView = findViewById(R.id.pageNumberTextView)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) //Set toolbar as the action bar
        supportActionBar?.title = "My PDF Viewer"

        // Initialize gesture detector
        gestureDetector = GestureDetectorCompat(this, this)

        // Set OnTouchListener for the ImageView
        imageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true // Consume the touch event
        }

        // Retrofit setup moved here
        apiService = retrofit().create(ApiService::class.java)
        loadPdfFromUrl()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Handle settings button click
                Log.d("MainActivity", "Settings button clicked")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun retrofit() : Retrofit {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit
    }

    private fun loadPdfFromUrl() {
        CoroutineScope(Dispatchers.IO).launch {
            // Use IO dispatcher for network call

            val call = apiService.getPdfData("$baseUrl/pdfs/$pdfFileName")

            withContext(Dispatchers.Main) {  // Switch to Main thread for UI updates
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            response.body()?.let { body ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val pdfFile = savePdfToCache(this@MainActivity, body, pdfFileName)
                                        withContext(Dispatchers.Main) {
                                            openPdf(pdfFile)
                                        }
                                    }
                                    catch (e: Exception) {
                                        Log.e("MainActivity", "Error saving PDF: ${e.message}", e)
                                        // Clean up
                                        CoroutineScope(Dispatchers.Main).launch {
                                            loadingTextView.text = "Error opening PDF"
                                            loadingProgressBar.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.e("MainActivity", "Failed to fetch workbooks: ${response.errorBody()?.string()}")
                            loadingTextView.text = "Error rendering page"
                            loadingProgressBar.visibility = View.GONE
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("MyFragment", "Network error: ${t.message}")
                        loadingTextView.text = "Error with network request"
                        loadingProgressBar.visibility = View.GONE
                    }
                })
            }
        }
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
        var renderer: PdfRenderer? = null
        try {
            descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor) // Local Variable

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
                currentPageIndex = index // Update current page index
                updatePageNumberTextView()


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

    // Gesture detection methods
    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onFling(
        event1: MotionEvent?,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val flingThreshold = 100
        val velocityThreshold = 100
        if (event1 != null) {
            val diffX = event2.x - event1.x

            if (Math.abs(diffX) > flingThreshold && Math.abs(velocityX) > velocityThreshold) {
                if (diffX > 0) {
                    // Swipe right (previous page)
                    goToPreviousPage()
                } else {
                    // Swipe left (next page)
                    goToNextPage()
                }
                return true
            }
        }
        return false
    }

    override fun onLongPress(event: MotionEvent) {}

    override fun onScroll(
        event1: MotionEvent?,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onShowPress(event: MotionEvent) {}

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return false
    }
    private fun goToNextPage() {
        CoroutineScope(Dispatchers.Main).launch {
            val renderer = pdfRenderer ?: return@launch
            if (currentPageIndex < renderer.pageCount - 1) {
                displayPage(currentPageIndex + 1)
            }
        }
    }

    private fun goToPreviousPage() {
        CoroutineScope(Dispatchers.Main).launch {
            if (currentPageIndex > 0) {
                displayPage(currentPageIndex - 1)
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun updatePageNumberTextView() {
        pageNumberTextView.text = "${currentPageIndex + 1}"
    }
}
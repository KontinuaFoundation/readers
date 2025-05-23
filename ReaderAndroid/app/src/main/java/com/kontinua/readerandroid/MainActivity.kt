package com.kontinua.readerandroid

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.kontinua.readerandroid.NavbarManager.ChapterData
import com.kontinua.readerandroid.NavbarManager.WorkbookData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

class MainActivity :
    AppCompatActivity(),
    GestureDetector.OnGestureListener {

    private lateinit var imageView: ImageView
    private lateinit var loadingTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var toolbar: Toolbar
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var pageNumberEditText: EditText
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var chapterView: NavigationView
    private lateinit var workbookView: NavigationView
    private lateinit var sidebar: FrameLayout
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPageIndex: Int = 0 // Track the current page index
    private lateinit var gestureDetector: GestureDetectorCompat
    private val baseUrl = "http://10.0.2.2:8000/"
    private var pdfFileName = "workbook-01.pdf"
    private var metaFileName = "workbook-01.json"
    private lateinit var annotationView: AnnotationView

    // timer variables
    private lateinit var timerBarLayout: LinearLayout
    private lateinit var timerControlsLayout: LinearLayout
    private lateinit var timerFillView: View
    private lateinit var pauseButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var restartButton: ImageButton
    private var timer: CountDownTimer? = null
    private var timerDuration: Long = 0
    private var timeLeftMillis: Long = timerDuration
    private var isTimerRunning: Boolean = false // state management
    private var elapsedTimeMillis: Long = 0
    private var fifteenminutes: Long = (15 * 60 * 10)
    private var twentyminutes: Long = (20 * 60 * 1000)
    private var twentyfiveminutes: Long = (25 * 60 * 1000)

    private lateinit var navbarManager: NavbarManager

    interface ApiService {
        @GET
        fun getPdfData(@Url url: String): Call<ResponseBody>

        @GET
        fun getChapters(@Url url: String): Call<List<ChapterData>>

        @GET("workbooks.json")
        fun getWorkbooks(): Call<List<WorkbookData>>
    }
    private lateinit var apiService: ApiService

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Set toolbar as the action bar
        supportActionBar?.title = ""

        // timer stuff
        timerBarLayout = findViewById(R.id.timerBarLayout)
        timerFillView = findViewById(R.id.timerFillView)
        timerControlsLayout = findViewById(R.id.timerControlsLayout) // ADD THIS LINE
        pauseButton = findViewById(R.id.pauseButton)
        cancelButton = findViewById(R.id.cancelButton)
        restartButton = findViewById(R.id.restartButton)
        // make the buttons listen
        pauseButton.setOnClickListener { pauseTimer() }
        cancelButton.setOnClickListener { cancelTimer() }
        restartButton.setOnClickListener { restartTimer() }
        // Initially hide the timer controls and timer bar
        timerControlsLayout.visibility = View.GONE
        timerBarLayout.visibility = View.GONE

        imageView = findViewById(R.id.pdfImageView)
        loadingTextView = findViewById(R.id.loadingTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
        pageNumberEditText = findViewById(R.id.pageNumberEditText)
        chapterView = findViewById(R.id.chapter_view)
        workbookView = findViewById(R.id.workbook_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        annotationView = findViewById(R.id.drawingView)
        sidebar = findViewById(R.id.sidebar_container)

        val openWorkbookNavButton = findViewById<Button>(R.id.open_workbook_nav_button)

        // Initialize gesture detector
        gestureDetector = GestureDetectorCompat(this, this)

        // Set OnTouchListener for the ImageView
        imageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true // Consume the touch event
        }

        // Handle user input in EditText when they press "Enter"
        pageNumberEditText.setOnKeyListener(
            View.OnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    // Get the entered page number
                    val pageNumberString = pageNumberEditText.text.toString()

                    // Validate the input and navigate to the page
                    if (pageNumberString.isNotEmpty()) {
                        try {
                            val pageNumber = pageNumberString.toInt()
                            goToPage(pageNumber - 1) // Subtract 1 because PDF pages are 0-indexed
                        } catch (e: NumberFormatException) {
                            // Handle invalid input (e.g., show an error message)
                            Log.e("MainActivity", "Invalid page number format")
                        }
                    }
                    return@OnKeyListener true // Consume the event
                }
                false // Don't consume the event
            },
        )

        // Retrofit setup moved here
        apiService = retrofit().create(ApiService::class.java)
        loadPdfFromUrl()

        // Initialize SidebarManager
        navbarManager = NavbarManager(this, drawerLayout, chapterView, workbookView, apiService, sidebar)
        navbarManager.setupNavbar(toolbar)
        navbarManager.loadChaptersFromServer(baseUrl, metaFileName)
        navbarManager.loadWorkbooks()

        openWorkbookNavButton.setOnClickListener {
            if (navbarManager.getWorkbookSelected()) {
                navbarManager.setWorkbookSelected(false)
                sidebar.layoutParams.width = 960
                chapterView.animate()
                    .translationXBy(400f)
                    .setDuration(300)
                    .withEndAction {
                        // Show workbook_view in its place
                        workbookView.visibility = View.VISIBLE
                        workbookView.alpha = 0f
                        workbookView.animate().alpha(1f).setDuration(300).start()
                    }
                    .start()
            } else {
                navbarManager.setWorkbookSelected(true)
                chapterView.animate()
                    .translationX(0f)
                    .setDuration(300)
                    .withEndAction {
                        sidebar.layoutParams.width = 560
                        // Hide workbook_view
                        workbookView.animate().alpha(0f).setDuration(300).withEndAction {
                            workbookView.visibility = View.GONE
                        }.start()
                    }
                    .start()
            }
        }

        previousButton.setOnClickListener {
            goToPreviousPage()
        }

        nextButton.setOnClickListener {
            goToNextPage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_timer -> {
            showTimerMenu(findViewById(R.id.action_timer))
            // Handle timer button click
            Log.d("MainActivity", "timer button clicked")
            true
        }
        R.id.action_markup -> {
            showMarkupMenu(findViewById(R.id.action_markup))
            // Handle markup button click
            Log.d("MainActivity", "markup button clicked")
            true
        }
        R.id.action_resources -> {
            showResourcesMenu(findViewById(R.id.action_resources))

            // Handle resources button click
            Log.d("MainActivity", "resources button clicked")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // Function to show the popup menu
    private fun showMarkupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.markup_menu, popup.menu)

        // Handle menu item clicks
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_pen -> {
                    annotationView.setDrawingMode(true, "pen", Color.BLACK)
                    true
                }
                R.id.action_highlight -> {
                    annotationView.setDrawingMode(true, "highlight", Color.YELLOW)
                    true
                }
                R.id.action_eraser -> {
                    annotationView.setEraseMode(true) // Enable eraser mode
                    true
                }
                R.id.action_clear -> {
                    annotationView.clearCanvas() // Clear the canvas
                    true
                }
                R.id.action_exit -> {
                    annotationView.setDrawingMode(false, "exit", Color.TRANSPARENT)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    // Function to show the popup menu for timer
    private fun showTimerMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.timer_menu, popup.menu)
        // Handle menu item clicks
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_15mins -> {
                    setTimerDuration(fifteenminutes) // actually shorter for testing
                    startTimer()
                    true
                }
                R.id.action_20mins -> {
                    setTimerDuration(twentyminutes)
                    startTimer()
                    true
                }
                R.id.action_25mins -> {
                    setTimerDuration(twentyfiveminutes)
                    startTimer()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun setTimerDuration(durationMillis: Long) {
        timerDuration = durationMillis
        timeLeftMillis = durationMillis // Reset time left to the new duration
    }

    // Function to show the popup menu
    // does not currently actually do anything
    private fun showResourcesMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.resources_menu, popup.menu)

        // Handle menu item clicks
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_article -> {
                    true
                }
                R.id.action_video -> {
                    annotationView.setDrawingMode(true, "highlight", Color.YELLOW)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun retrofit(): Retrofit {
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

            withContext(Dispatchers.Main) {
                // Switch to Main thread for UI updates
                call.enqueue(object : Callback<ResponseBody> {
                    @SuppressLint("SetTextI18n")
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            response.body()?.let { body ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val pdfFile = savePdfToCache(this@MainActivity, body, pdfFileName)
                                        withContext(Dispatchers.Main) {
                                            openPdf(pdfFile)
                                        }
                                    } catch (e: Exception) {
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

                    @SuppressLint("SetTextI18n")
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
                displayPage(0) // Load the first page
                annotationView.loadAnnotations() // Load annotations on startup
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
            val renderer = pdfRenderer // Local Variable

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
                updatePageNumberEditText()

                loadingTextView.visibility = View.GONE // Hide the loading message
                loadingProgressBar.visibility = View.GONE // Hide the progress bar
                imageView.visibility = View.VISIBLE // Show the PDF
            } catch (e: Exception) {
                Log.e("MainActivity", "Error rendering page: ${e.message}")
                loadingTextView.text = "Error Rendering PDF"
                loadingProgressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (timer != null) {
            timer?.cancel()
        }
        annotationView.saveAnnotations()
        Log.d("MainActivity", "Saved Annotations to")
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error closing PDF resources: ${e.message}")
        }
    }

    // Gesture detection methods
    override fun onDown(event: MotionEvent): Boolean = true

    override fun onFling(event1: MotionEvent?, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
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

    override fun onScroll(event1: MotionEvent?, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean =
        false

    override fun onShowPress(event: MotionEvent) {}

    override fun onSingleTapUp(event: MotionEvent): Boolean = false
    private fun goToNextPage() {
        CoroutineScope(Dispatchers.Main).launch {
            val renderer = pdfRenderer ?: return@launch
            if (currentPageIndex < renderer.pageCount - 1) {
                annotationView.setPage(currentPageIndex + 1)
                displayPage(currentPageIndex + 1)
            }
        }
    }

    private fun goToPreviousPage() {
        CoroutineScope(Dispatchers.Main).launch {
            if (currentPageIndex > 0) {
                annotationView.setPage(currentPageIndex - 1)
                displayPage(currentPageIndex - 1)
            }
        }
    }
    private fun goToPage(pageNumber: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val renderer = pdfRenderer ?: return@launch
            if (pageNumber >= 0 && pageNumber < renderer.pageCount) {
                annotationView.setPage(pageNumber)
                displayPage(pageNumber)
            } else {
                // Handle invalid page number (e.g., show an error message)
                Log.e("MainActivity", "Invalid page number entered")
            }
        }
    }

    @SuppressLint("ServiceCast")
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("SetTextI18n")
    private fun updatePageNumberEditText() {
        pageNumberEditText.setText((currentPageIndex + 1).toString())
    }

    private fun startTimer() {
        isTimerRunning = true
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val timerControlsLayout = toolbar.findViewById<LinearLayout>(R.id.timerControlsLayout)
        timerControlsLayout.visibility = View.VISIBLE
        timerBarLayout.visibility = View.VISIBLE
        timerFillView.setBackgroundColor(ContextCompat.getColor(this, R.color.my_green))
        pauseButton.setImageResource(R.drawable.ic_pause) // make button the pause button
        elapsedTimeMillis = 0 // this is critical to the calculation

        timer = object : CountDownTimer(timeLeftMillis, 100) { // update every 100 miliseconds
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                elapsedTimeMillis = timerDuration - timeLeftMillis
                updateTimerBar()
            }

            override fun onFinish() {
                isTimerRunning = false
                val toolbar = findViewById<Toolbar>(R.id.toolbar)
                val timerControlsLayout = toolbar.findViewById<LinearLayout>(R.id.timerControlsLayout)
                timerControlsLayout.visibility = View.GONE
                timerBarLayout.visibility = View.GONE
                timeLeftMillis = timerDuration
                elapsedTimeMillis = timerDuration
                updateTimerBar()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (isTimerRunning) {
            timer?.cancel()
            isTimerRunning = false
            pauseButton.setImageResource(R.drawable.ic_resume) // make the pause button back to play
            timerFillView.setBackgroundColor(ContextCompat.getColor(this, R.color.my_yellow))
        } else {
            startTimer()
            pauseButton.setImageResource(R.drawable.ic_pause)
            timerFillView.setBackgroundColor(ContextCompat.getColor(this, R.color.my_green))
        }
    }

    private fun restartTimer() {
        timer?.cancel()
        timeLeftMillis = timerDuration
        startTimer()
    }

    private fun cancelTimer() {
        timer?.cancel()
        isTimerRunning = false
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val timerControlsLayout = toolbar.findViewById<LinearLayout>(R.id.timerControlsLayout)
        timerControlsLayout.visibility = View.GONE
        timerBarLayout.visibility = View.GONE
        timeLeftMillis = timerDuration
        updateTimerBar()
        val params = timerFillView.layoutParams as LinearLayout.LayoutParams
        params.weight = 0.0f // makes the bar go away
        timerFillView.layoutParams = params // resets the timer bar to full
    }

    private fun updateTimerBar() {
        val progress = (elapsedTimeMillis).toFloat() / timerDuration.toFloat() // elapsed time
        val params = timerFillView.layoutParams as LinearLayout.LayoutParams
        params.weight = progress
        timerFillView.layoutParams = params
    }

    fun loadPageNumber(pageNumber: Int) {
        currentPageIndex = pageNumber
        annotationView.setPage(pageNumber - 1)
        displayPage(pageNumber - 1)
    }

    fun loadNewWorkbook(workbook: WorkbookData) {
        pdfFileName = workbook.pdfName
        metaFileName = workbook.metaName
        navbarManager.loadChaptersFromServer(baseUrl, metaFileName)
        loadPdfFromUrl()
        annotationView.setWorkbook(pdfFileName)
    }
}

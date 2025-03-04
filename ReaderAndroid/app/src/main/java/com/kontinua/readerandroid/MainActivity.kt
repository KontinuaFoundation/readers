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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
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
    GestureDetector.OnGestureListener,
    NavigationView.OnNavigationItemSelectedListener {

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
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPageIndex: Int = 0 // Track the current page index
    private lateinit var gestureDetector: GestureDetectorCompat
    private val baseUrl = "http://10.0.2.2:8000/"
    private var pdfFileName = "workbook-01.pdf"
    private var metaFileName = "workbook-01.json"
    private val chapterMap = mutableMapOf<Int, Int>()
    private val workbookMap = mutableMapOf<Int, WorkbookData>()
    private var workbookSelected = true
    private lateinit var annotationView: AnnotationView

    //timer variables
    private lateinit var timerBarLayout: LinearLayout
    private lateinit var timerControlsLayout: LinearLayout
    private lateinit var timerFillView: View

    private lateinit var pauseButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var restartButton: ImageButton
    private var timer: CountDownTimer? = null
    private var timerDuration: Long = 300 //hrd coded
    private var timeLeftMillis: Long = timerDuration //hard coded
    private var isTimerRunning: Boolean = false //state management
    private var isTimerPaused: Boolean = false


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

        //timer stuff
        timerBarLayout = findViewById(R.id.timerBarLayout);
        timerFillView = findViewById(R.id.timerFillView);
        timerControlsLayout = findViewById(R.id.timerControlsLayout) // ADD THIS LINE

        pauseButton = findViewById(R.id.pauseButton)
        cancelButton = findViewById(R.id.cancelButton)
        restartButton = findViewById(R.id.restartButton)

        pauseButton.setOnClickListener { pauseTimer() }
        cancelButton.setOnClickListener { cancelTimer() }
        restartButton.setOnClickListener { restartTimer() }

        // Initially hide the timer controls and show timer bar
        timerControlsLayout.visibility = View.GONE
        timerBarLayout.visibility = View.VISIBLE

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
        loadChaptersFromServer()

        openWorkbookNavButton.setOnClickListener {
            if (workbookSelected) {
                loadWorkbooks()
                workbookSelected = false
            } else {
                loadChaptersFromServer()
                workbookSelected = true
            }
        }

        previousButton.setOnClickListener {
            goToPreviousPage()
        }

        nextButton.setOnClickListener {
            goToNextPage()
        }

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close,
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        chapterView.setNavigationItemSelectedListener(this)
        workbookView.setNavigationItemSelectedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            else -> {
                if (workbookSelected) {
                    Log.d("MainActivity", "Clicked on Chapter: ${item.title}")
                    currentPageIndex = chapterMap[item.itemId]!!
                    annotationView.setPage(currentPageIndex - 1)
                    displayPage(currentPageIndex - 1)
                } else {
                    Log.d("MainActivity", "Clicked on Workbook: ${item.title}")
                    pdfFileName = workbookMap[item.itemId]!!.pdfName
                    metaFileName = workbookMap[item.itemId]!!.metaName
                    currentPageIndex = 0
                    workbookSelected = true
                    loadChaptersFromServer()
                    loadPdfFromUrl()
                    annotationView.setWorkbook(pdfFileName)
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
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
                    setTimerDuration(15 * 60 * 10) // 15 minutes in milliseconds
                    startTimer()
                    true
                }
                R.id.action_20mins -> {
                    setTimerDuration(20 * 60 * 1000) // 20 minutes in milliseconds
                    startTimer()
                    true
                }
                R.id.action_25mins -> {
                    setTimerDuration(25 * 60 * 1000) // 25 minutes in milliseconds
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
    //does not currently actually do anything
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
                displayPage(pageNumber)
                annotationView.setPage(currentPageIndex)
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

    data class ChapterData(val title: String, val id: String, val chap_num: Int, val start_page: Int)
    data class WorkbookData(val id: String, val metaName: String, val pdfName: String)

    private fun populateMenu(chapters: List<ChapterData>) {
        val menu = chapterView.menu
        menu.clear() // Clear existing items

        for (chapter in chapters) {
            val menuItem = menu.add(
                Menu.NONE,
                chapter.id.hashCode(),
                Menu.NONE,
                "${chapter.chap_num}. ${chapter.title}",
            )
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER) // Ensure it stays in the sidebar
            chapterMap[chapter.id.hashCode()] = chapter.start_page
        }
    }

    private fun loadChaptersFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getChapters("$baseUrl/meta/$metaFileName").execute()

                if (response.isSuccessful) {
                    val chapters = response.body()
                    Log.d("MainActivity", "Server Response: $chapters")

                    withContext(Dispatchers.Main) {
                        if (chapters != null) {
                            populateMenu(chapters)
                        } else {
                            Log.e("MainActivity", "Chapters data is NULL")
                        }
                    }
                } else {
                    Log.e("MainActivity", "Failed to fetch chapters: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Network Error: ${e.message}")
            }
        }
    }

    private fun populateWorkbookMenu(workbooks: List<WorkbookData>) {
        val menu = chapterView.menu
        menu.clear()

        for (workbook in workbooks) {
            val menuItem = menu.add(Menu.NONE, workbook.id.hashCode(), Menu.NONE, workbook.id)
            workbookMap[workbook.id.hashCode()] = workbook
        }
    }

    private fun startTimer() {
        isTimerRunning = true
        timerControlsLayout.visibility = View.VISIBLE
        pauseButton.setImageResource(R.drawable.ic_pause) //Ensure pause icon is set

        timer = object : CountDownTimer(timeLeftMillis, 100) { // Update every 100 milliseconds
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                updateTimerBar()
            }

            override fun onFinish() {
                isTimerRunning = false
                timerControlsLayout.visibility = View.GONE
                timeLeftMillis = timerDuration // Reset for next use, if needed
                updateTimerBar() //Set to full, or empty depending on desired finish state

            }
        }.start()
    }
    private fun pauseTimer() {
        if (isTimerRunning) {
            timer?.cancel()
            isTimerRunning = false
            pauseButton.setImageResource(R.drawable.ic_resume) //Change to play icon
        } else {
            startTimer()
            pauseButton.setImageResource(R.drawable.ic_pause)
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
        timerControlsLayout.visibility = View.GONE
        timeLeftMillis = timerDuration
        updateTimerBar() // Reset the timer bar to empty
    }

    private fun updateTimerBar() {
        val progress = (timerDuration - timeLeftMillis).toFloat() / timerDuration.toFloat() // calculate elapsed time
        val params = timerFillView.layoutParams as LinearLayout.LayoutParams
        params.weight = progress
        timerFillView.layoutParams = params
    }
    private fun loadWorkbooks() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiService.getWorkbooks().execute()

            if (response.isSuccessful) {
                val workbooks = response.body()
                if (workbooks != null) {
                    withContext(Dispatchers.Main) {
                        populateWorkbookMenu(workbooks)
                    }
                }
            } else {
                Log.e("MainActivity", "Failed to load workbooks: ${response.errorBody()?.string()}")
            }
        }
    }
}

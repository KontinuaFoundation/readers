package com.kontinua.readerandroid

import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NavbarManager(
    private val context: Context,
    private val drawerLayout: DrawerLayout,
    private val chapterView: NavigationView,
    private val workbookView: NavigationView,
    private val apiService: MainActivity.ApiService,
    private val sidebar: FrameLayout
) : NavigationView.OnNavigationItemSelectedListener {
    private var workbookSelected = true
    private val chapterMap = mutableMapOf<Int, Int>()
    private val workbookMap = mutableMapOf<Int, WorkbookData>()

    data class ChapterData(val title: String, val id: String, val chap_num: Int, val start_page: Int)
    data class WorkbookData(val id: String, val metaName: String, val pdfName: String)

    fun setupNavbar(toolbar: androidx.appcompat.widget.Toolbar) {
        val toggle = object : ActionBarDrawerToggle(
            context as MainActivity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ){
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)

                // If the second menu is open, reset back to one menu
                if (sidebar.width == 960) {
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
                    workbookSelected = true
                }
            }
        }
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        chapterView.setNavigationItemSelectedListener(this)
        workbookView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        when {
            chapterMap.containsKey(itemId) -> { // It's a Chapter
                Log.d("NavbarManager", "Clicked on Chapter: ${item.title}")
                val page = chapterMap[itemId]!!
                (context as MainActivity).loadPageNumber(page)
            }

            workbookMap.containsKey(itemId) -> { // It's a Workbook
                Log.d("NavbarManager", "Clicked on Workbook: ${item.title}")
                val workbook = workbookMap[itemId]!!
                (context as MainActivity).loadNewWorkbook(workbook)
            }

            else -> {
                Log.e("NavbarManager", "Unknown item clicked: ${item.title}")
            }
        }
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
        workbookSelected = true
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

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

    private fun populateWorkbookMenu(workbooks: List<WorkbookData>) {
        val menu = workbookView.menu
        menu.clear()

        for (workbook in workbooks) {
            val menuItem = menu.add(Menu.NONE, -workbook.id.hashCode(), Menu.NONE, workbook.id)
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            workbookMap[-workbook.id.hashCode()] = workbook
        }
    }

    fun loadChaptersFromServer(baseUrl: String, metaFileName: String) {
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

    fun loadWorkbooks() {
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

    fun getWorkbookSelected(): Boolean {
        return workbookSelected
    }

    fun setWorkbookSelected(selected: Boolean) {
        workbookSelected = selected
    }
}

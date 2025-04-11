package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel

//molly changes
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class NavbarManager {
    var isChapterVisible by mutableStateOf(false)
        private set

    var isWorkbookVisible by mutableStateOf(false)
        private set

    var collectionVM: CollectionViewModel? by mutableStateOf(null)
        private set

//    var pageNumber: Int = 0
//        private set

    var currentPage by mutableIntStateOf(0)
        private set

    // Total pages in the current document - Make it observable
    var totalPages by mutableIntStateOf(0) // Start with 0, update when PDF loads
        private set

    init {
        isChapterVisible = false
        isWorkbookVisible = false
        collectionVM = null
    }

    fun toggleChapterSidebar() {
        isChapterVisible = !isChapterVisible
    }

    fun toggleWorkbookSidebar() {
        isWorkbookVisible = !isWorkbookVisible
    }

    fun closeSidebar() {
        isChapterVisible = false
        isWorkbookVisible = false
    }

    fun setCollection(collection: CollectionViewModel?) {
        this.collectionVM = collection
    }

    // --- Page Navigation Methods ---

    /**
     * Updates the current page index and total page count.
     * Should be called by the PDFViewer when the PDF is loaded or the page changes internally.
     * @param newPage The new current page index (0-based).
     * @param total The total number of pages.
     */
    fun updatePageInfo(newPage: Int, total: Int) {
        // Run on Main thread if called from background potentially (though callbacks usually on main)
        CoroutineScope(Dispatchers.Main).launch {
            if (total > 0) { // Only update if total is valid
                totalPages = total
                // Clamp newPage to valid range
                currentPage = newPage.coerceIn(0, (total - 1).coerceAtLeast(0))
            } else {
                // Reset if PDF is unloaded or invalid
                totalPages = 0
                currentPage = 0
            }
        }
    }


    /**
     * Sets the desired page. The PDFViewer should observe `currentPage` and react.
     * @param targetPage The desired page index (0-based).
     */
    fun goToPage(targetPage: Int) {
        // Run on Main thread for state updates
        CoroutineScope(Dispatchers.Main).launch {
            if (totalPages > 0 && targetPage in 0 until totalPages) {
                currentPage = targetPage
            }
            // Optionally add logging or user feedback for invalid page numbers
        }
    }

    /**
     * Navigates to the next page if possible.
     */
    fun goToNextPage() {
        if (currentPage < totalPages - 1) {
            goToPage(currentPage + 1)
        }
    }

    /**
     * Navigates to the previous page if possible.
     */
    fun goToPreviousPage() {
        if (currentPage > 0) {
            goToPage(currentPage - 1)
        }
    }

    /**
     * Resets page count when a new document might be loading.
     */
    fun resetPages() {
        CoroutineScope(Dispatchers.Main).launch {
            currentPage = 0
            totalPages = 0
        }
    }
    // --- End Page Navigation Methods ---

    // Keep old setPage for compatibility if needed, but prefer goToPage
    // DEPRECATED - Use goToPage
    fun setPage(newPage: Int){
        goToPage(newPage)
    }

    // Getter for the old pageNumber property if anything still uses it
    // DEPRECATED - Use currentPage
    val pageNumber: Int
        get() = currentPage

}
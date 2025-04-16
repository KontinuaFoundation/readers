package com.kontinua.readersandroidjetpack.util

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel

class NavbarManager {
    var isChapterVisible by mutableStateOf(false)
        private set

    var isWorkbookVisible by mutableStateOf(false)
        private set

    var collectionVM: CollectionViewModel? by mutableStateOf(null)
        private set

    var currentChapterIndex by mutableIntStateOf(-1)
        private set

    var pageNumber: Int = 0
        private set

//    var pageNumber by mutableIntStateOf(0)
//        private set

    private var pageCount: Int = -1

    init {
        pageNumber = 0
        pageCount = -1
        // Initialize state vars
        isChapterVisible = false
        isWorkbookVisible = false
        collectionVM = null
        currentChapterIndex = -1 // Explicitly init state
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
        if (this.collectionVM != collection) {
            this.collectionVM = collection
        }
        updateChapter()
    }

    fun setPageCount(newPageCount: Int){
        pageCount = newPageCount
        updateChapter()
    }

    fun setPage(newPage: Int) {
        val potentialPage = if (pageCount > 0) newPage.coerceIn(0, pageCount - 1) else newPage.coerceAtLeast(0)
        if (potentialPage != pageNumber) {
            pageNumber = potentialPage // Update the internal variable
            Log.d("NavbarManager", "setPage: Updated internal pageNumber to $pageNumber")
            updateChapter()
        } else {
            updateChapter()
        }
    }

    fun goToNextPage() {
        if (pageNumber < pageCount - 1) {
            setPage(pageNumber + 1)
        }
    }

    fun goToPreviousPage() {
        if (pageNumber > 0) {
            setPage(pageNumber - 1)
        }
    }

    fun getCurrentChapter(): Chapter? {
        if (currentChapterIndex < 0) {
            return null
        }
        val chapters = collectionVM?.chaptersState?.value
        val chapter = chapters?.getOrNull(currentChapterIndex)
        return chapter
    }

    private fun updateChapter() {
        val chapters = collectionVM?.chaptersState?.value
        val currentPage = pageNumber // Use the internal variable for calculation

        // Log inputs
        // Log.v("NavbarManager", "updateChapter: Calculating for page $currentPage. Chapters: ${chapters?.size ?: "null"}. Current Index State: $currentChapterIndex")

        if (chapters == null || chapters.isEmpty()) {
            // Reset index state if no chapters
            if (currentChapterIndex != -1) { // Only update state if it changes
                Log.d("NavbarManager", "updateChapter: No chapters, resetting index state to -1")
                currentChapterIndex = -1
            }
            return
        }

        // --- Use the reliable linear scan from end ---
        val startPages = chapters.map { it.startPage - 1 } // 0-indexed
        var calculatedIndex = -1 // Default to -1
        for (i in chapters.indices.reversed()) {
            if (currentPage >= startPages[i]) {
                calculatedIndex = i
                break // Found the correct chapter index
            }
        }
        // --- End Scan ---

        // Log calculation result
        // Log.v("NavbarManager", "updateChapter: Calculated index $calculatedIndex for page $currentPage.")

        // --- Update the State ONLY if the calculated index is different ---
        // This comparison IS correct. If the index hasn't changed, we DON'T need to update the state.
        // The recomposition should happen because the caller (e.g., MainScreen) is observing the state.
        if (calculatedIndex != currentChapterIndex) {
            Log.d("NavbarManager", "updateChapter: Index changed! Updating STATE from $currentChapterIndex to $calculatedIndex.")
            currentChapterIndex = calculatedIndex // *** This assignment updates the State ***
        }
    }
}
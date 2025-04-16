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

    var currentChapterIndex: Int = -1
        private set

//    var pageNumber: Int = 0
//        private set
// --- MAKE pageNumber State ---
    var pageNumber by mutableIntStateOf(0)
        private set

    private var pageCount: Int = -1

    init {
        pageNumber = 0
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
        //idk if this is necessary
        updateChapter()
    }

    fun setPageCount(newPageCount: Int){
        pageCount = newPageCount
        Log.d("pages", "New page count: $pageCount")
    }

    fun setPage(newPage: Int){
        val clampedPage = if (pageCount > 0) newPage.coerceIn(0, pageCount - 1) else newPage.coerceAtLeast(0)
        if (clampedPage != pageNumber) {
            pageNumber = clampedPage // Update State -> Triggers recomposition
            updateChapter() // Update index based on new page and CURRENT chapter list
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
        val chapters = collectionVM?.chaptersState?.value // Read StateFlow value
        val chapter = chapters?.getOrNull(currentChapterIndex)
        // Log.v("NavbarManager", "getCurrentChapter called: index=$currentChapterIndex, Chapter=${chapter?.chapNum}")
        return chapter
    }

    private fun updateChapter() {
        val chapters = collectionVM?.chaptersState?.value ?: run { // Read StateFlow value
            if (currentChapterIndex != -1) { currentChapterIndex = -1 }
            return
        }
        if (chapters.isEmpty()) {
            if (currentChapterIndex != -1) { currentChapterIndex = -1 }
            return
        }

        val startPages = chapters.map { it.startPage - 1 } // 0-indexed
        // Ensure startPages is sorted if using binarySearch, otherwise linear scan is safer
        // Linear scan implementation:
        var foundIndex = -1
        for (i in chapters.indices.reversed()) {
            if (pageNumber >= startPages[i]) {
                foundIndex = i
                break
            }
        }
        // Binary search implementation (ONLY if startPages is sorted):
        // val index = startPages.binarySearch(pageNumber)
        // val foundIndex = if (index >= 0) index else (-index - 2).coerceIn(-1, chapters.lastIndex)


        if (foundIndex != currentChapterIndex) {
            Log.d("NavbarManager", "Updating chapter index from $currentChapterIndex to $foundIndex for page $pageNumber")
            currentChapterIndex = foundIndex
        }
    }
}
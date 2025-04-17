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

    var currentChapterIndex: Int = 0
        private set

    var pageNumber by mutableIntStateOf(0)
        private set

    var pageCount by mutableIntStateOf(-1)
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

    fun setPageCountValue(newPageCount: Int){
        pageCount = newPageCount
    }

    fun setPage(newPage: Int){
        pageNumber = newPage
        updateChapter()
    }

    fun goToNextPage() {
        if (pageNumber < pageCount) {
            setPage(pageNumber + 1)
        }
    }

    fun goToPreviousPage() {
        if (pageNumber > 0) {
            setPage(pageNumber - 1)
        }
    }

    fun getCurrentChapter(): Chapter? {
        return if(currentChapterIndex >= 0) collectionVM?.chapters?.get(currentChapterIndex) else null
    }

    fun getAdjustedPage(): String {
        return (pageNumber + 1).toString()
    }

    private fun updateChapter() {
        val startPages = collectionVM?.chapters?.map { it.startPage - 1} ?: emptyList()
        val index = startPages.binarySearch(pageNumber)

        currentChapterIndex = if (index >= 0) {
            // pageNumber exactly matches a chapter start page.
            index
        } else {
            // Compute the insertion point: (-index - 1), and then adjust by subtracting 1.
            // This gives the index of the start page that is immediately less than pageNumber.
            (-index - 2).coerceIn(-1, startPages.lastIndex)
        }
    }

    fun getCurrentChapter(): Chapter? {
        val chapters = collectionVM?.chapters ?: return null

        // Find the chapter that contains the current page
        return chapters.findLast { it.startPage <= pageNumber + 1 }
    }
}
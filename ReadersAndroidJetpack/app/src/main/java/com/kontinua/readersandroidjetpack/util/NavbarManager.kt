package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
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

    var pageNumber: Int = 0
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

    fun setPage(newPage: Int){
        pageNumber = newPage
        updateChapter()
    }

    fun getCurrentChapter(): Chapter? {
        return if(currentChapterIndex >= 0) collectionVM?.chapters?.get(currentChapterIndex) else null
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
}
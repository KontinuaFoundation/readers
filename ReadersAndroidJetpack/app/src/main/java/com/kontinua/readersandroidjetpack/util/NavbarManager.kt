package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
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

    var currentChapterIndex: Int = 0
        private set

    var pageNumber: Int = 0
        private set

    var currentPage by mutableIntStateOf(0)
        private set

    var totalPages by mutableIntStateOf(0)

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
        goToPage(newPage)
        updateChapter()

    }

    fun updatePageInfo(newPage: Int, total: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            if (total > 0) {
                totalPages = total
                currentPage = newPage.coerceIn(0, (total - 1).coerceAtLeast(0))
            } else {
                totalPages = 0
                currentPage = 0
            }
        }
    }

    private fun goToPage(targetPage: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            if (totalPages > 0 && targetPage in 0 until totalPages) {
                currentPage = targetPage
            }
        }
    }

    fun goToNextPage() {
        if (currentPage < totalPages - 1) {
            setPage(currentPage + 1)
        }
    }

    fun goToPreviousPage() {
        if (currentPage > 0) {
            setPage(currentPage - 1)
        }
    }

    fun resetPages() {
        CoroutineScope(Dispatchers.Main).launch {
            currentPage = 0
            totalPages = 0
        }
    }

    fun getCurrentChapter(): Chapter? {
        return if(currentChapterIndex >= 0) collectionVM?.chapters?.get(currentChapterIndex) else null
    }

    private fun updateChapter() {
        val startPages = collectionVM?.chapters?.map { it.startPage - 1} ?: emptyList()
        val index = startPages.binarySearch(pageNumber)

        currentChapterIndex = if (index >= 0) {
            index
        } else {
            (-index - 2).coerceIn(-1, startPages.lastIndex)
        }
    }
}
package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel

class NavbarManager {
    var isChapterVisible by mutableStateOf(false)
        private set

    var isWorkbookVisible by mutableStateOf(false)
        private set

    var chapterClicked by mutableStateOf(false)
        private set

    var collectionVM: CollectionViewModel? by mutableStateOf(null)
        private set

    var pageNumber by  mutableIntStateOf(0)

    var currentWorkbook: String

    init {
        isChapterVisible = false
        isWorkbookVisible = false
        chapterClicked = false
        collectionVM = null
        currentWorkbook = "Workbook 1"
    }

    fun toggleChapterSidebar() {
        isChapterVisible = !isChapterVisible
    }

    fun toggleWorkbookSidebar() {
        isWorkbookVisible = !isWorkbookVisible
    }

    fun setClicked(boolean: Boolean) {
        chapterClicked = boolean
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
    }

    fun setWorkbook(newWorkbook: String){
        currentWorkbook = newWorkbook
    }
}
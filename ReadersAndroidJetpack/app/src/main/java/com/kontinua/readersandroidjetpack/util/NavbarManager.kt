package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.serialization.Workbook
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel

class NavbarManager {
    var isChapterVisible by mutableStateOf(false)
        private set

    var isWorkbookVisible by mutableStateOf(false)
        private set

    var collectionVM: CollectionViewModel? by mutableStateOf(null)
        private set

    var pageNumber: Int = 0
        private set

    var currentWorkbook: String

    init {
        isChapterVisible = false
        isWorkbookVisible = false
        collectionVM = null
        currentWorkbook = "Workbook 1"
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
    }

    fun setWorkbook(newWorkbook: String){
        currentWorkbook = newWorkbook
    }
}
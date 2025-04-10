package com.kontinua.readersandroidjetpack.util

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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

    private fun updateChapter() {
        val startPages = collectionVM?.chapters?.map { it.startPage - 1} ?: emptyList()

        if(pageNumber < startPages[0]){
            currentChapterIndex = -1
        }else if(pageNumber >= startPages[startPages.lastIndex]){
            currentChapterIndex = startPages.lastIndex
        } else{
            for(i in 0..startPages.size - 2){
                if(startPages[i] <= pageNumber && startPages[i + 1] > pageNumber){
                    currentChapterIndex = i
                    break;
                }
            }
        }
    }

    fun setPage(newPage: Int){
        pageNumber = newPage
        Log.d("pages", "New page: $pageNumber")
        updateChapter()
    }
}
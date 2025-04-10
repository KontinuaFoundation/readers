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

    var currentChapter: Chapter? by mutableStateOf(null)
        private set

    var pageNumber: Int = 0
        private set

    init {
        isChapterVisible = false
        isWorkbookVisible = false
        collectionVM = null
        currentChapter = null
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
        Log.d("pages", startPages.toString())
        if(pageNumber < startPages[0]){
            currentChapter = null
        }else if(pageNumber >= startPages[startPages.lastIndex]){
            currentChapter = collectionVM?.chapters?.get(startPages.lastIndex)
        } else{
            for(i in 0..startPages.size - 2){
                if(startPages[i] <= pageNumber && startPages[i + 1] > pageNumber){
                    currentChapter = collectionVM?.chapters?.get(i)
                }
            }
        }
        Log.d("pages", "New chapter: $currentChapter")
    }

    fun setPage(newPage: Int){
        pageNumber = newPage
        Log.d("pages", "New page: $pageNumber")
        updateChapter()
    }
}
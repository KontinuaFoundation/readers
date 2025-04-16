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

    //needs to be state
    var currentChapterIndex by mutableIntStateOf(-1)

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
        currentChapterIndex = -1
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

//    fun setPageCount(newPageCount: Int){
//        pageCount = newPageCount
//        Log.d("pages", "New page count: $pageCount")
//    }
fun setPageCount(newPageCount: Int){
    if (newPageCount != pageCount) {
        pageCount = newPageCount
        Log.d("NavbarManager", "Set pageCount: $pageCount")
        // Clamp page if needed
        if (pageCount in 1..pageNumber) {
            val clampedPage = pageCount - 1
            if (clampedPage != pageNumber) { // Avoid infinite loop if setPage calls setPageCount
                pageNumber = clampedPage
                updateChapter()
            }
        }
    }
}

//    fun setPage(newPage: Int){
//        val clampedPage = if (pageCount > 0) newPage.coerceIn(0, pageCount - 1) else newPage.coerceAtLeast(0)
//        if (clampedPage != pageNumber) {
//            pageNumber = clampedPage // Update State -> Triggers recomposition
//            updateChapter() // Update index based on new page and CURRENT chapter list
//        }
//    }

    // Updates internal pageNumber variable and triggers index calculation
    fun setPage(newPage: Int){
        val clampedPage = if (pageCount > 0) newPage.coerceIn(0, pageCount - 1) else newPage.coerceAtLeast(0)

        // Update internal variable only if it changed
        if (clampedPage != pageNumber) {
            pageNumber = clampedPage
            // Log.v("NavbarManager", "Set internal pageNumber var: $pageNumber")
            updateChapter() // Calculate and potentially update the index STATE
        } else {
            // If page didn't change, still run updateChapter in case
            // chapter list changed concurrently.
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
        val chapters = collectionVM?.chaptersState?.value // Read StateFlow value
        val chapter = chapters?.getOrNull(currentChapterIndex)
        return chapter
    }

//    private fun updateChapter() {
//        val chapters = collectionVM?.chaptersState?.value ?: run { // Read StateFlow value
//            if (currentChapterIndex != -1) { currentChapterIndex = -1 }
//            return
//        }
//        if (chapters.isEmpty()) {
//            if (currentChapterIndex != -1) { currentChapterIndex = -1 }
//            return
//        }
//
//        val startPages = chapters.map { it.startPage - 1 } // 0-indexed
//        // Ensure startPages is sorted if using binarySearch, otherwise linear scan is safer
//        // Linear scan implementation:
//        var foundIndex = -1
//        for (i in chapters.indices.reversed()) {
//            if (pageNumber >= startPages[i]) {
//                foundIndex = i
//                break
//            }
//        }
//        // Binary search implementation (ONLY if startPages is sorted):
//        // val index = startPages.binarySearch(pageNumber)
//        // val foundIndex = if (index >= 0) index else (-index - 2).coerceIn(-1, chapters.lastIndex)
//
//
//        if (foundIndex != currentChapterIndex) {
//            Log.d("NavbarManager", "Updating chapter index from $currentChapterIndex to $foundIndex for page $pageNumber")
//            currentChapterIndex = foundIndex
//        }
//    }
//}
// Calculates index based on internal pageNumber and updates index STATE
private fun updateChapter() {
    val chapters = collectionVM?.chaptersState?.value
    val currentPage = pageNumber // Use internal variable for calculation

    if (chapters == null || chapters.isEmpty()) {
        if (currentChapterIndex != -1) { // Only update state if it changes
            currentChapterIndex = -1
        }
        return
    }

    val startPages = chapters.map { it.startPage - 1 }
    var calculatedIndex = -1
    for (i in chapters.indices.reversed()) {
        if (currentPage >= startPages[i]) {
            calculatedIndex = i
            break
        }
    }

    // --- Update the currentChapterIndex STATE only if calculated index is different ---
    if (calculatedIndex != currentChapterIndex) {
        Log.d("NavbarManager", "Updating chapter index STATE from $currentChapterIndex to $calculatedIndex for internal page $currentPage")
        currentChapterIndex = calculatedIndex // *** Assignment updates State ***
    }
}
}
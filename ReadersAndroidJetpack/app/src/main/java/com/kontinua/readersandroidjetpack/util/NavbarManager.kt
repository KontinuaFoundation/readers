package com.kontinua.readersandroidjetpack.util
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

    //needs to be state var for dig resources
    var currentChapterIndex by mutableIntStateOf(-1)

    var pageNumber: Int = 0
        private set

    private var pageCount: Int = -1

    init {
        pageNumber = 0
        pageCount = -1
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

    fun setPageCount(newPageCount: Int){
        pageCount = newPageCount
    }

    fun setPage(newPage: Int){
        pageNumber = newPage
        updateChapter()
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
        val chapters = collectionVM?.chaptersState?.value
        val chapter = chapters?.getOrNull(currentChapterIndex)
        return chapter
    }


private fun updateChapter() {
    val chapters = collectionVM?.chaptersState?.value
    val currentPage = pageNumber

    if (chapters.isNullOrEmpty()) {
        if (currentChapterIndex != -1) {
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
        if (calculatedIndex != currentChapterIndex) {
            currentChapterIndex = calculatedIndex
        }
    }
}
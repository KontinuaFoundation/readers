package com.kontinua.readersandroidjetpack.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NavbarManager {
    var isChapterVisible by mutableStateOf(false)
        private set

    var isWorkbookVisible by mutableStateOf(false)
        private set

    var collectionVM: CollectionViewModel? by mutableStateOf(null)
        private set

    var currentChapterIndex by mutableIntStateOf(-1)
        private set

    var pageNumber by mutableIntStateOf(0)
        private set

    var pageCount by mutableIntStateOf(-1)
        private set

    var searchManager = SearchManager()
        private set

    // added so there are no race conditions failing with mainactivty and loading and stuff
    // tbh no 100% sure why it's happening anyway, but this fixed it
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // needed for preferences and loading stuff
    private var prefs: UserPreferencesRepository? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isInitialized = false

    /**
     * new init for the manager async  using the saved
     * preferences from the last time the app was used
     */
    fun initialize(context: Context, collectionViewModel: CollectionViewModel) {
        if (isInitialized) return
        isInitialized = true

        this.prefs = UserPreferencesRepository(context)
        this.collectionVM = collectionViewModel

        scope.launch {
            val collection = collectionViewModel.collectionState.filterNotNull().first()
            if (collection.workbooks.isEmpty()) {
                _isLoading.value = false
                return@launch
            }

            val lastWorkbookId = prefs?.getLastWorkbookId() ?: -1
            val workbookToLoad = collection.workbooks.find { it.id == lastWorkbookId }
                ?: collection.workbooks.first()

            val lastPage = prefs?.getPageForWorkbook(workbookToLoad.id) ?: 0
            setPage(lastPage, persist = false)

            collectionViewModel.setWorkbook(workbookToLoad)
            collectionViewModel.workbookState.filterNotNull().first { it.id == workbookToLoad.id }

            _isLoading.value = false
        }
    }

    /**
     * Called when the user selects a new workbook from the sidebar
     * saves the new workbook id and loads the last viewed page for it
     */
    fun onWorkbookChanged(newWorkbook: WorkbookPreview) {
        prefs?.saveLastWorkbookId(newWorkbook.id)

        // default to first page if never opened before
        val lastPage = prefs?.getPageForWorkbook(newWorkbook.id) ?: 0
        setPage(lastPage, persist = false)

        collectionVM?.setWorkbook(newWorkbook)
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

    fun setPageCountValue(newPageCount: Int) {
        pageCount = newPageCount
    }

    // updated to save w persistence
    fun setPage(newPage: Int, persist: Boolean = true) {
        if (pageNumber != newPage) {
            pageNumber = newPage
            updateChapter()
            if (persist && collectionVM?.currentWorkbook != null) {
                prefs?.savePageForWorkbook(collectionVM!!.currentWorkbook.id, newPage)
            }
        }
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
        return if (currentChapterIndex >= 0) collectionVM?.chapters?.getOrNull(currentChapterIndex) else null
    }

    fun getAdjustedPage(): String {
        return (pageNumber + 1).toString()
    }

    private fun updateChapter() {
        val startPages = collectionVM?.chapters?.map { it.startPage - 1 } ?: emptyList()
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

package com.kontinua.readersandroidjetpack.util
import androidx.compose.runtime.getValue
import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel

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

    // for persistence
    private var prefs: UserPreferencesRepository? = null
    private var isInitialized = false

//    init {
//        isChapterVisible = false
//        isWorkbookVisible = false
//        collectionVM = null
//    }

    /**
     * Initializes the manager with context to load user preferences.
     * This should be called once from a Composable with access to the context.
     */
    fun initialize(context: Context, collectionVM: CollectionViewModel) {
        if (isInitialized) return
        this.prefs = UserPreferencesRepository(context)
        setCollection(collectionVM)

        val collection = collectionVM.collectionState.value ?: return

        // 1. Determine the workbook to load
        val lastWorkbookId = prefs?.getLastWorkbookId()
        val workbookToLoad = collection.workbooks.find { it.id == lastWorkbookId }
            ?: collection.workbooks.first()

        // 2. Load the last viewed page for that workbook
        val lastPage = prefs?.getPageForWorkbook(workbookToLoad.id) ?: 0
        setPage(lastPage, persist = false) // Don't re-save on init

        // 3. Tell the ViewModel to fetch this workbook's data
        collectionVM.setWorkbook(workbookToLoad)
        isInitialized = true
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

    fun setPageCountValue(newPageCount: Int) {
        pageCount = newPageCount
    }

//    fun setPage(newPage: Int) {
//        pageNumber = newPage
//        updateChapter()
//    }
    /**
     * Sets the current page and persists it if required.
     */
    fun setPage(newPage: Int, persist: Boolean = true) {
        if (pageNumber != newPage) {
            pageNumber = newPage
            updateChapter()
            if (persist && collectionVM != null) {
                prefs?.savePageForWorkbook(collectionVM!!.currentWorkbook.id, newPage)
            }
        }
    }

    /**
     * Called when the user selects a new workbook from the sidebar.
     * It loads the last viewed page for that workbook.
     */
    fun onWorkbookChanged(newWorkbook: WorkbookPreview) {
        // Save this as the most recently used workbook
        prefs?.saveLastWorkbookId(newWorkbook.id)

        // Load the last known page for this new workbook, defaulting to 0
        val lastPage = prefs?.getPageForWorkbook(newWorkbook.id) ?: 0
        setPage(lastPage, persist = false) // Set page without re-saving
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
        return if (currentChapterIndex >= 0) collectionVM?.chapters?.get(currentChapterIndex) else null
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

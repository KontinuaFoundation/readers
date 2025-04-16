package com.kontinua.readersandroidjetpack.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.util.APIManager
import com.kontinua.readersandroidjetpack.serialization.Collection
import com.kontinua.readersandroidjetpack.serialization.Workbook
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel : ViewModel() {

    private val _collectionState = MutableStateFlow<Collection?>(null)
    private val _workbookState = MutableStateFlow<Workbook?>(null)
    private val _chaptersState = MutableStateFlow<List<Chapter>>(emptyList())

    val collectionState: StateFlow<Collection?> = _collectionState.asStateFlow()
    val workbookState: StateFlow<Workbook?> = _workbookState.asStateFlow()
    val chaptersState: StateFlow<List<Chapter>> = _chaptersState.asStateFlow()
//    lateinit var currentWorkbook: WorkbookPreview
    // Track the PREVIEW of the currently loaded workbook
    var currentWorkbookPreview: WorkbookPreview? = null
        private set // Keep track of what was requested
//    var chapters: List<Chapter> = emptyList()

    init {
        viewModelScope.launch {
            val latestCollection = APIManager.getLatestCollection()

            if (latestCollection == null) {
                Log.e("Collection", "Fetching latest collection returned null.")
                return@launch
            }

            updateCollection(latestCollection)

            latestCollection.workbooks.firstOrNull()?.let { firstPreview ->
                Log.d("CollectionViewModel", "Init: Loading first workbook: ${firstPreview.id}")
                setWorkbook(firstPreview) // Load the first workbook
            } ?: Log.w("CollectionViewModel", "Init: Collection has no workbooks.")
        }

    }

    private fun updateCollection(newCollection: Collection) {
        _collectionState.value = newCollection
    }

    private fun updateWorkbook(workbook: Workbook) {
        /*
        Sets the current workbook. Do not call directly, instead use setWorkbook
         */
        _workbookState.value = workbook
        _chaptersState.value = workbook.chapters
    }

    fun setWorkbook(preview: WorkbookPreview) {
        // --- FIX: Prevent redundant fetches (addresses slowness) ---
        if (preview.id == currentWorkbookPreview?.id && _workbookState.value?.id == preview.id) {
            Log.d("CollectionViewModel", "setWorkbook: Workbook ${preview.id} is already loaded. Skipping fetch.")
            // Optionally re-emit state if needed, though usually not necessary if skipping
            // _workbookState.value?.let { processLoadedWorkbook(it) }
            return
        }
        // --- End FIX ---

        Log.d("CollectionViewModel", "setWorkbook: Requesting workbook ${preview.id}")
        // Update the requested preview immediately
        currentWorkbookPreview = preview

        // Clear previous state while loading? Optional, depends on desired UX
        // _workbookState.value = null
        // _chaptersState.value = emptyList()

        viewModelScope.launch {
            val workbook = APIManager.getWorkbook(preview)

            if (workbook == null) {
                Log.e("CollectionViewModel", "setWorkbook: Fetching workbook ${preview.id} returned null.")
                // Optionally clear state on failure
                // currentWorkbookPreview = null
                // _workbookState.value = null
                // _chaptersState.value = emptyList()
                return@launch
            }
            Log.d("CollectionViewModel", "setWorkbook: Workbook ${workbook.id} fetched successfully.")
            // Process the loaded data, updating states (_workbookState, _chaptersState)
            updateWorkbook(workbook)
        }
    }
}
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

    private var currentWorkbookPreview: WorkbookPreview? = null

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
        if (preview.id == currentWorkbookPreview?.id && _workbookState.value?.id == preview.id) {
            Log.d("CollectionViewModel", "setWorkbook: Workbook ${preview.id} is already loaded. Skipping fetch.")
            return
        }
        Log.d("CollectionViewModel", "setWorkbook: Requesting workbook ${preview.id}")
        currentWorkbookPreview = preview
        viewModelScope.launch {
            val workbook = APIManager.getWorkbook(preview)

            if (workbook == null) {
                Log.e("CollectionViewModel", "setWorkbook: Fetching workbook ${preview.id} returned null.")
                return@launch
            }
            Log.d("CollectionViewModel", "setWorkbook: Workbook ${workbook.id} fetched successfully.")
            updateWorkbook(workbook)
        }
    }
}
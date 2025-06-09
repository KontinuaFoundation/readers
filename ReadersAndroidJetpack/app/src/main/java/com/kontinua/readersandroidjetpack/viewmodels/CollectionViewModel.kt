package com.kontinua.readersandroidjetpack.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.serialization.Collection
import com.kontinua.readersandroidjetpack.serialization.Workbook
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.util.APIManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel : ViewModel() {

    private val _collectionState = MutableStateFlow<Collection?>(null)
    val collectionState: StateFlow<Collection?> = _collectionState.asStateFlow()

    private val _workbookState = MutableStateFlow<Workbook?>(null)
    val workbookState: StateFlow<Workbook?> = _workbookState.asStateFlow()
    lateinit var currentWorkbook: WorkbookPreview
    var chapters: List<Chapter> = emptyList()

    init {
        viewModelScope.launch {
            // no longer setting default workbook here
            // navbar manager will tell it which workbook to load
            val latestCollection = APIManager.getLatestCollection()

            // this should not happen but like just in case
            if (latestCollection == null) {
                Log.e("Collection", "Fetching latest collection returned null.")
                return@launch
            }
            _collectionState.value = latestCollection
        }
    }

    private fun updateWorkbook(workbook: Workbook) {
        /*
        Sets the current workbook. Do not call directly, instead use setWorkbook
         */
        _workbookState.value = workbook
    }

    fun setWorkbook(preview: WorkbookPreview) {
        /*
        Sets the current workbook by fetching the workbook based on its preview.
        Previews should come from collection.workbooks
         */

        viewModelScope.launch {
            val workbook = APIManager.getWorkbook(preview)

            if (workbook == null) {
                Log.e("Workbook", "Fetching workbook $preview.id returned null.")
                return@launch
            }

            currentWorkbook = preview
            chapters = workbook.chapters
            updateWorkbook(workbook)
        }
    }
}

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

    private val _Workbook_collectionState = MutableStateFlow<Collection?>(null)
    private val _workbookState = MutableStateFlow<Workbook?>(null)

    val collectionState: StateFlow<Collection?> = _Workbook_collectionState.asStateFlow()
    val workbookState: StateFlow<Workbook?> = _workbookState.asStateFlow()
    lateinit var currentWorkbook: WorkbookPreview
    var chapters: List<Chapter> = emptyList()

    init {
        viewModelScope.launch {
            val latestCollection = APIManager.getLatestCollection()

            if (latestCollection == null) {
                Log.e("Collection", "Fetching latest collection returned null.")
                return@launch
            }

            updateCollection(latestCollection)
            // Default to the first workbook for now...
            setWorkbook(latestCollection.workbooks.first())
        }

    }

    private fun updateCollection(newCollection: Collection) {
        _Workbook_collectionState.value = newCollection
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
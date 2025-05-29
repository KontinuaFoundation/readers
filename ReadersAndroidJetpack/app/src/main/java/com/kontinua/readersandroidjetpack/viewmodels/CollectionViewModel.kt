package com.kontinua.readersandroidjetpack.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private val _workbookState = MutableStateFlow<Workbook?>(null)

    val collectionState: StateFlow<Collection?> = _collectionState.asStateFlow()
    val workbookState: StateFlow<Workbook?> = _workbookState.asStateFlow()
    var currentWorkbookPreview: WorkbookPreview? by mutableStateOf(null)
    private set

    var chapters: List<Chapter> by mutableStateOf(emptyList())
        private set

    init {
        fetchLatestCollection()
    }

    private fun fetchLatestCollection(onCollectionLoaded: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (_collectionState.value == null) {
                val latestCollection = APIManager.getLatestCollection()
                if (latestCollection != null) {
                    _collectionState.value = latestCollection
                }
            }
            onCollectionLoaded?.invoke()
        }
    }

    private fun updateWorkbookInternal(workbook: Workbook, preview: WorkbookPreview) {
        _workbookState.value = workbook
        this.currentWorkbookPreview = preview
        this.chapters = workbook.chapters
    }

    private fun setWorkbookByPreview(preview: WorkbookPreview) {
        viewModelScope.launch {
            val workbook = APIManager.getWorkbook(preview)

            if (workbook == null) {
                _workbookState.value = null
                this@CollectionViewModel.currentWorkbookPreview = null
                this@CollectionViewModel.chapters = emptyList()
                return@launch
            }
            updateWorkbookInternal(workbook, preview)
        }
    }

    fun loadWorkbookById(workbookIdString: String) {
        val workbookId = workbookIdString.toIntOrNull()
        if (workbookId == null) {
            loadDefaultWorkbook()
            return
        }

        fetchLatestCollection {
            val currentCollection = _collectionState.value
            if (currentCollection == null) {
                loadDefaultWorkbookAfterCheck()
                return@fetchLatestCollection
            }

            val previewToLoad = currentCollection.workbooks.find { it.id == workbookId }
            if (previewToLoad != null) {
                setWorkbookByPreview(previewToLoad)
            } else {
                loadDefaultWorkbookAfterCheck()
            }
        }
    }

    fun loadDefaultWorkbook() {
        fetchLatestCollection {
            loadDefaultWorkbookAfterCheck()
        }
    }
    private fun loadDefaultWorkbookAfterCheck() {
        val currentCollection = _collectionState.value
        if (currentCollection != null && currentCollection.workbooks.isNotEmpty()) {
            val firstWorkbookPreview = currentCollection.workbooks.first()
            setWorkbookByPreview(firstWorkbookPreview)
        } else {
            _workbookState.value = null
            this.currentWorkbookPreview = null
            this.chapters = emptyList()
        }
    }

//    private fun updateCollection(newCollection: Collection) {
//        _collectionState.value = newCollection
//    }

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

            currentWorkbookPreview = preview
            chapters = workbook.chapters
            updateWorkbook(workbook)
        }
    }
}

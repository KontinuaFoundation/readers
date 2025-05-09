package com.kontinua.readersandroidjetpack.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kontinua.readersandroidjetpack.data.BookmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BookmarkRepository(application.applicationContext)

    private val _bookmarkLookup = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    val bookmarkLookup: StateFlow<Map<Int, Set<Int>>> = _bookmarkLookup.asStateFlow()

    init {
        viewModelScope.launch {
            // Load initial bookmarks from DataStore and keep observing
            repository.bookmarkLookupFlow.collect { loadedBookmarks ->
                _bookmarkLookup.value = loadedBookmarks
            }
        }
    }

//    fun isBookmarked(workbookId: Int, currentPage: Int): Boolean {
//        return _bookmarkLookup.value[workbookId]?.contains(currentPage) ?: false
//    }

    fun toggleBookmark(workbookId: Int, currentPage: Int) {
        _bookmarkLookup.update { currentLookup ->
            val mutableLookup = currentLookup.toMutableMap()
            val pagesForWorkbook = mutableLookup[workbookId]?.toMutableSet() ?: mutableSetOf()

            if (pagesForWorkbook.contains(currentPage)) {
                pagesForWorkbook.remove(currentPage)
            } else {
                pagesForWorkbook.add(currentPage)
            }

            if (pagesForWorkbook.isEmpty()) {
                mutableLookup.remove(workbookId)
            } else {
                mutableLookup[workbookId] = pagesForWorkbook.toSet()
            }
            mutableLookup.toMap()
        }
        // Save to DataStore after every toggle
        viewModelScope.launch {
            repository.saveBookmarkLookup(_bookmarkLookup.value)
        }
    }
}

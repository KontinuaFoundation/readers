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
            // Load initial bookmarks from DataStore and observe
            repository.bookmarkLookupFlow.collect { loadedBookmarks ->
                _bookmarkLookup.value = loadedBookmarks
            }
        }
    }

    fun toggleBookmark(workbookId: Int, currentPage: Int) {
        _bookmarkLookup.update { currentLookup ->
            currentLookup.toMutableMap().apply {
                val pagesForWorkbook = getOrPut(workbookId) { mutableSetOf() }.toMutableSet()
                pagesForWorkbook.toggle(currentPage)
                if (pagesForWorkbook.isEmpty()) {
                    remove(workbookId)
                } else {
                    put(workbookId, pagesForWorkbook.toSet())
                }
            }
        }
        // Save to DataStore after every toggle
        viewModelScope.launch {
            repository.saveBookmarkLookup(_bookmarkLookup.value)
        }
    }
}

// jeremy requested change
private fun <T> MutableSet<T>.toggle(v: T) = this.add(v) || this.remove(v)

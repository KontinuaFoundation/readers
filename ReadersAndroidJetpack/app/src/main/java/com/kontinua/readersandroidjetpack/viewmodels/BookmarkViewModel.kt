package com.kontinua.readersandroidjetpack.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookmarkViewModel : ViewModel() {
    // Private mutable state flow
    private val _bookmarkLookup = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    // Public immutable state flow for Composables to observe
    val bookmarkLookup: StateFlow<Map<Int, Set<Int>>> = _bookmarkLookup.asStateFlow()

    init {
        // TODO: Load bookmarks from persistent storage (e.g., SharedPreferences, DataStore, Room)
        // For example:
        // viewModelScope.launch {
        //     _bookmarkLookup.value = loadBookmarksFromPersistence()
        // }
    }

// is current page bookmarked?
    fun isBookmarked(workbookId: Int, currentPage: Int): Boolean {
        return _bookmarkLookup.value[workbookId]?.contains(currentPage) ?: false
    }

// toggle bookmark status
    fun toggleBookmark(workbookId: Int, currentPage: Int) {
        _bookmarkLookup.update { currentLookup ->
            val mutableLookup = currentLookup.toMutableMap() // Create a mutable copy of the map
            val pagesForWorkbook = mutableLookup[workbookId]?.toMutableSet() ?: mutableSetOf() // Get existing set or create new

            if (pagesForWorkbook.contains(currentPage)) {
                pagesForWorkbook.remove(currentPage)
            } else {
                pagesForWorkbook.add(currentPage)
            }

            if (pagesForWorkbook.isEmpty()) {
                mutableLookup.remove(workbookId) // Clean up if no bookmarks left for this workbook
            } else {
                mutableLookup[workbookId] = pagesForWorkbook.toSet() // Store as immutable set
            }
            mutableLookup.toMap() // Return the new immutable map for the StateFlow
        }
        // TODO: Save bookmarks to persistent storage after modification
        // For example:
        // viewModelScope.launch {
        //     saveBookmarksToPersistence(_bookmarkLookup.value)
        // }
    }

    // --- Example Persistence (using SharedPreferences, very basic) ---
    // You'd typically use DataStore or Room for more complex data.
    // This is a placeholder to illustrate the concept.
    /*
    private fun saveBookmarksToPersistence(bookmarks: Map<Int, Set<Int>>) {
        // Implementation depends on your chosen storage method
        // e.g., convert map to JSON String and save to SharedPreferences
        Log.d("BookmarkViewModel", "Saving bookmarks: $bookmarks")
    }

    private suspend fun loadBookmarksFromPersistence(): Map<Int, Set<Int>> {
        // Implementation depends on your chosen storage method
        // e.g., load JSON String from SharedPreferences and parse
        Log.d("BookmarkViewModel", "Loading bookmarks...")
        return emptyMap() // Replace with actual loading logic
    }
    */
}

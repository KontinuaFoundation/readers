package com.kontinua.readersandroidjetpack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer

// Create an extension property for DataStore
val Context.bookmarkDataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks_preferences")

class BookmarkRepository(private val context: Context) {

    private object PreferencesKeys {
        val BOOKMARK_LOOKUP_JSON = stringPreferencesKey("bookmark_lookup_json")
    }

    // Define the serializer for your specific map type
    private val mapSerializer = MapSerializer(Int.serializer(), SetSerializer(Int.serializer()))

    val bookmarkLookupFlow: Flow<Map<Int, Set<Int>>> = context.bookmarkDataStore.data
        .map { preferences ->
            val jsonString = preferences[PreferencesKeys.BOOKMARK_LOOKUP_JSON]
            if (jsonString != null) {
                try {
                    Json.decodeFromString(mapSerializer, jsonString)
                } catch (e: Exception) {
                    // Handle deserialization error, e.g., log and return empty map
                    emptyMap<Int, Set<Int>>()
                }
            } else {
                emptyMap<Int, Set<Int>>()
            }
        }

    suspend fun saveBookmarkLookup(bookmarks: Map<Int, Set<Int>>) {
        val jsonString = Json.encodeToString(mapSerializer, bookmarks)
        context.bookmarkDataStore.edit { preferences ->
            preferences[PreferencesKeys.BOOKMARK_LOOKUP_JSON] = jsonString
        }
    }

    // Optional: if you need a one-time fetch outside of a flow
    suspend fun getBookmarksOnce(): Map<Int, Set<Int>> {
        return bookmarkLookupFlow.firstOrNull() ?: emptyMap()
    }
}

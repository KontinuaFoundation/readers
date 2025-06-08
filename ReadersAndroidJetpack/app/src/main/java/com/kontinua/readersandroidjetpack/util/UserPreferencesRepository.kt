package com.kontinua.readersandroidjetpack.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * A repository for managing user preferences, such as last viewed page and workbook,
 * using SharedPreferences for persistence.
 *
 * This version correctly handles Integer workbook IDs.
 */
class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Saves the last viewed workbook ID.
     */
    fun saveLastWorkbookId(id: Int) {
        prefs.edit() { putInt(KEY_LAST_WORKBOOK_ID, id) }
    }

    /**
     * Retrieves the last viewed workbook ID.
     * @return The saved workbook ID, or -1 if none is saved.
     */
    fun getLastWorkbookId(): Int {
        // Return -1 as a sentinel value indicating no ID was found.
        return prefs.getInt(KEY_LAST_WORKBOOK_ID, -1)
    }

    /**
     * Saves the page number for a specific workbook ID.
     */
    fun savePageForWorkbook(workbookId: Int, page: Int) {
        prefs.edit() { putInt(getWorkbookPageKey(workbookId), page) }
    }

    /**
     * Retrieves the page number for a specific workbook ID.
     * @return The saved page number, or 0 (the first page) if none is saved.
     */
    fun getPageForWorkbook(workbookId: Int): Int {
        return prefs.getInt(getWorkbookPageKey(workbookId), 0)
    }

    /**
     * Generates a unique SharedPreferences key for a workbook's page.
     */
    private fun getWorkbookPageKey(workbookId: Int): String {
        return "${KEY_WORKBOOK_PAGE_PREFIX}_$workbookId"
    }

    companion object {
        private const val PREFS_NAME = "reader_app_prefs"
        private const val KEY_LAST_WORKBOOK_ID = "last_workbook_id"
        private const val KEY_WORKBOOK_PAGE_PREFIX = "page_for_workbook"
    }
}

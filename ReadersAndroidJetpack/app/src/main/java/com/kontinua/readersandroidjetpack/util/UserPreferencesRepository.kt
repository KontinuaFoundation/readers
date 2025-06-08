package com.kontinua.readersandroidjetpack.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.kontinua.readersandroidjetpack.R

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        (R.string.PREFS_NAME.toString()),
        Context.MODE_PRIVATE
    )

    fun saveLastWorkbookId(id: Int) {
        prefs.edit() { putInt(R.string.KEY_LAST_WORKBOOK_ID.toString(), id) }
    }

    fun getLastWorkbookId(): Int {
        // -1 as default if not found
        return prefs.getInt(R.string.KEY_LAST_WORKBOOK_ID.toString(), -1)
    }

    fun savePageForWorkbook(workbookId: Int, page: Int) {
        prefs.edit() { putInt(getWorkbookPageKey(workbookId), page) }
    }

    fun getPageForWorkbook(workbookId: Int): Int {
        return prefs.getInt(getWorkbookPageKey(workbookId), 0)
    }

    private fun getWorkbookPageKey(workbookId: Int): String {
        return "${R.string.KEY_WORKBOOK_PAGE_PREFIX}_$workbookId"
    }
}

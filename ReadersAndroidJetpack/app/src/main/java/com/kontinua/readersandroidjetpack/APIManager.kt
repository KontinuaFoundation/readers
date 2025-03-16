package com.kontinua.readersandroidjetpack

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.kontinua.readersandroidjetpack.models.CollectionPreview
import com.kontinua.readersandroidjetpack.models.Collection
import com.kontinua.readersandroidjetpack.models.Workbook
import com.kontinua.readersandroidjetpack.models.WorkbookPreview

object APIManager {

    private const val API_URL = "http://18.189.208.93/api"
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()

    suspend fun getCollections(localization: String = "en-US"): List<CollectionPreview>? {
        val type = Types.newParameterizedType(List::class.java, CollectionPreview::class.java)
        val adapter = moshi.adapter<List<CollectionPreview>>(type)

        return withContext(Dispatchers.IO) {

            val request =
                Request.Builder().url("$API_URL/collections?localization=$localization").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")
                return@withContext response.body?.string()?.let { adapter.fromJson(it) }
            }

        }
    }

    suspend fun getCollection(preview: CollectionPreview): Collection? {
        /*
        Gets detailed collection given its preview
         */
        val adapter = moshi.adapter(Collection::class.java)

        val id = preview.id

        val request = Request.Builder().url("$API_URL/collections/$id").build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")
                return@withContext response.body?.string()?.let { adapter.fromJson(it) }
            }


        }
    }

    suspend fun getLatestCollection(localization: String = "en-US"): Collection? {
        val collections = getCollections(localization)

        if (collections == null) {
            return null
        }

        val latestCollectionPreview = collections.first()

        return getCollection(latestCollectionPreview)
    }

    suspend fun getWorkbook(preview: WorkbookPreview): Workbook? {

        val adapter = moshi.adapter(Workbook::class.java)

        val id = preview.id

        val request = Request.Builder().url("$API_URL/workbooks/$id").build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")

                return@withContext response.body?.string()?.let { adapter.fromJson(it) }
            }


        }
    }

}

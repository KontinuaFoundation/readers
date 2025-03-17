package com.kontinua.readersandroidjetpack.util

import android.util.Log
import com.kontinua.readersandroidjetpack.Constants.API_URL
import com.kontinua.readersandroidjetpack.serialization.Collection
import com.kontinua.readersandroidjetpack.serialization.CollectionPreview
import com.kontinua.readersandroidjetpack.serialization.Workbook
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream


object APIManager {
    /*
    Manages any requests pertaining to the backend Kontinua Readers Service
    i.e. fetching workbooks/collections and downloading pdfs.
     */

    private final val CLIENT = OkHttpClient()
    private final val MOSHI = Moshi.Builder().build()

    suspend fun getCollections(
        localization: String = "en-US",
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<CollectionPreview>? {
        /*
        Fetches all collections for a given localization.
         */
        val type = Types.newParameterizedType(List::class.java, CollectionPreview::class.java)
        val adapter = MOSHI.adapter<List<CollectionPreview>>(type)

        return withContext(dispatcher) {

            val request =
                Request.Builder().url("$API_URL/collections?localization=$localization").build()

            CLIENT.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")
                return@withContext response.body?.string()?.let { adapter.fromJson(it) }
            }

        }
    }

    suspend fun getCollection(
        preview: CollectionPreview,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Collection? {
        /*
        Given a preview, fetches the collection which will include which workbooks it offers.
         */
        val adapter = MOSHI.adapter(Collection::class.java)

        val id = preview.id

        val request = Request.Builder().url("$API_URL/collections/$id").build()

        return withContext(dispatcher) {
            CLIENT.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")
                return@withContext response.body?.string()?.let { adapter.fromJson(it) }
            }


        }
    }

    suspend fun getLatestCollection(
        localization: String = "en-US",
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Collection? {
        val collections = getCollections(localization, dispatcher)

        if (collections == null) {
            return null
        }

        val latestCollectionPreview = collections.first()

        return getCollection(latestCollectionPreview, dispatcher)
    }

    suspend fun getWorkbook(
        preview: WorkbookPreview,

        dispatcher: CoroutineDispatcher = Dispatchers.IO


    ): Workbook? {

        val adapter = MOSHI.adapter(Workbook::class.java)

        val id = preview.id

        val request = Request.Builder().url("$API_URL/workbooks/$id").build()

        return withContext(dispatcher) {
            CLIENT.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")

                return@withContext response.body?.string()?.let { adapter.fromJson(it) }
            }


        }
    }

    suspend fun getPDFFromWorkbook(
        context: android.content.Context, workbook: Workbook,

        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): File? {
        /*
        Given a workbook, downloads PDF and returns it s a file.
         */
        return withContext(dispatcher) {
            try {
                val request = Request.Builder().url(workbook.pdf).build()
                val response = CLIENT.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("Download PDF", "Failed: ${response.code}")
                    return@withContext null
                }

                response.body?.let { body ->
                    val file = File(context.cacheDir, "downloaded.pdf")
                    val fos = FileOutputStream(file)
                    fos.use { output ->
                        output.write(body.bytes())
                    }
                    return@withContext file
                }
            } catch (e: Exception) {
                Log.e("Download PDF", "Error downloading PDF: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

}

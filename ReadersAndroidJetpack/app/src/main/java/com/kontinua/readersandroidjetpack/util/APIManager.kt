package com.kontinua.readersandroidjetpack.util

import android.util.Log
import com.kontinua.readersandroidjetpack.serialization.Collection
import com.kontinua.readersandroidjetpack.serialization.CollectionPreview
import com.kontinua.readersandroidjetpack.serialization.Workbook
import com.kontinua.readersandroidjetpack.serialization.WorkbookPreview
import com.kontinua.readersandroidjetpack.util.Constants.API_URL
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object APIManager {
    /*
    Manages any requests pertaining to the backend Kontinua Readers Service
    i.e. fetching workbooks/collections and downloading pdfs.
     */

    private val CLIENT = OkHttpClient()
    private val MOSHI = Moshi.Builder().build()

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

    private suspend fun getLatestCollectionPreview(
        localization: String = "en-US",
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): CollectionPreview? {
        /*
        Given a preview, fetches the collection which will include which workbooks it offers.
         */
        val adapter = MOSHI.adapter(CollectionPreview::class.java)

        val request = Request.Builder().url("$API_URL/collections/latest?localization=$localization").build()

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
        val latestCollectionPreview = getLatestCollectionPreview(localization) ?: return null

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

    suspend fun submitFeedback(
        workbookId: Int,
        chapterNumber: Int,
        pageNumber: Int,
        userEmail: String,
        description: String,
        majorVersion: Int,
        minorVersion: Int,
        localization: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Boolean {
        val feedbackUrl = "$API_URL/feedback/"

        Log.d("APIManager", "===== SUBMITTING FEEDBACK =====")
        Log.d("APIManager", "URL: $feedbackUrl")
        Log.d("APIManager", "Workbook ID: $workbookId")
        Log.d("APIManager", "Chapter Number: $chapterNumber")
        Log.d("APIManager", "Page Number: $pageNumber")
        Log.d("APIManager", "Email: $userEmail")
        Log.d("APIManager", "Description length: ${description.length}")
        Log.d("APIManager", "Version: $majorVersion.$minorVersion")
        Log.d("APIManager", "Localization: $localization")

        val json =
            JSONObject().apply {
                put("workbook_id", workbookId)
                put("chapter_number", chapterNumber)
                put("page_number", pageNumber)
                put("user_email", userEmail)
                put("description", description)
                put("major_version", majorVersion)
                put("minor_version", minorVersion)
                put("localization", localization)
            }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        Log.d("APIManager", "Building request...")
        val request =
            Request
                .Builder()
                .url(feedbackUrl)
                .post(requestBody)
                .build()

        Log.d("APIManager", "Request: ${request.method} ${request.url}")

        return withContext(dispatcher) {
            try {
                Log.d("APIManager", "Executing request...")
                val response = CLIENT.newCall(request).execute()
                val responseCode = response.code
                val responseBody = response.body?.string() ?: "Empty body"

                Log.d("APIManager", "Response code: $responseCode")
                Log.d("APIManager", "Response body: $responseBody")

                val isSuccessful = response.isSuccessful
                if (isSuccessful) {
                    Log.d("APIManager", "Request successful")
                } else {
                    Log.e("APIManager", "Failed to submit feedback: $responseCode")
                }

                Log.d("APIManager", "===== FEEDBACK SUBMISSION COMPLETE =====")
                isSuccessful
            } catch (e: Exception) {
                Log.e("APIManager", "Error submitting feedback", e)
                Log.e("APIManager", "Stack trace: ${e.stackTraceToString()}")
                Log.d("APIManager", "===== FEEDBACK SUBMISSION FAILED =====")
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun getPDFFromWorkbook(
        context: android.content.Context,
        workbook: Workbook,
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

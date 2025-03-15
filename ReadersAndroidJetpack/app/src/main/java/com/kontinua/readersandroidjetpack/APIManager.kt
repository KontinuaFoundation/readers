package com.kontinua.readersandroidjetpack

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.kontinua.readersandroidjetpack.models.Collection

object APIManager {

    private const val API_URL = "http://18.189.208.93/api"
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()

    private val type = Types.newParameterizedType(List::class.java, Collection::class.java)
    private val adapter = moshi.adapter<List<Collection>>(type)

    // Suspend function to make network call on IO thread
    suspend fun getCollections(): List<Collection>? {
        return withContext(Dispatchers.IO) {

            val request = Request.Builder().url("$API_URL/collections").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")
                return@withContext response.body?.string()?.let { adapter.fromJson(it) }
            }

        }
    }
}

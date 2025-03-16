package com.kontinua.readersandroidjetpack

import okhttp3.OkHttpClient

// TODO: Remove this when API manager is fully implemented.
object HttpClient {
    val instance: OkHttpClient by lazy {
        OkHttpClient()
    }
}
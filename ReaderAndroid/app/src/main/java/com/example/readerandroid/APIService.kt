package com.kontinua.readerandroid

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET("workbooks.json")
    fun getWorkbooks(): Call<List<Workbook>>

    @GET
    fun getPdfData(@Url url: String): Call<ResponseBody>
}
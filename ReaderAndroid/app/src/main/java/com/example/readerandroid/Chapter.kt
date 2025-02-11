package com.kontinua.readerandroid

import com.google.gson.annotations.SerializedName

data class Chapter(
    val id: String,
    val title: String,
    val book: String,
    @SerializedName("chap_num") val chapNum: Int,
    val covers: List<Cover>,
    @SerializedName("start_page") val startPage: Int,
    val requires: List<String>?
)

package com.kontinua.readersandroidjetpack.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Workbook(
    val id: Int,
    val number: Int,
    val chapters: List<Chapter>,
    val pdf: String,
    val collection: Int,
)

@JsonClass(generateAdapter = true)
data class Chapter(
    val title: String,
    val id: String,
    @Json(name = "chap_num")
    val chapNum: Int,
    @Json(name = "start_page")
    val startPage: Int,
    val covers: List<Cover>,
    val requires: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class Cover(
    val id: String,
    val desc: String,
    val videos: List<Video>? = null,
    val references: List<Reference>? = null
)

@JsonClass(generateAdapter = true)
data class Video(
    val link: String,
    val title: String
)

@JsonClass(generateAdapter = true)
data class Reference(
    val link: String,
    val title: String
)
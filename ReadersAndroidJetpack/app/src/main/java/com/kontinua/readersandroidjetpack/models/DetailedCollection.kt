package com.kontinua.readersandroidjetpack.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DetailedCollection(
    val id: Int,

    val workbooks: List<WorkbookPreview>,

    @Json(name = "major_version")
    val majorVersion: Int,

    @Json(name = "minor_version")
    val minorVersion: Int,

    val localization: String,

    @Json(name = "is_released")
    val isReleased: Boolean,

    @Json(name = "creation_date")
    val creationDate: String
)

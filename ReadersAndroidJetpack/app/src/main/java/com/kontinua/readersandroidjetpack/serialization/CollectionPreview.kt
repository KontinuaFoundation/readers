package com.kontinua.readersandroidjetpack.serialization

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// When we list collections.
@JsonClass(generateAdapter = true)
data class CollectionPreview(
    val id: Int,

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

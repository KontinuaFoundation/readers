package com.kontinua.readersandroidjetpack.serialization

import com.squareup.moshi.JsonClass

// When retrieving workbooks for a given collection.
@JsonClass(generateAdapter = true)
data class WorkbookPreview(
    val id: Int,
    val number: Int
)

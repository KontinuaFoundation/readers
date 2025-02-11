package com.kontinua.readerandroid
import com.google.gson.annotations.SerializedName


data class Workbook(
    val id: String,
    @SerializedName("metaName") val metaName: String,
    @SerializedName("pdfName") val pdfName: String
)
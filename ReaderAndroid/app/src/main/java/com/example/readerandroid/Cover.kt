package com.kontinua.readerandroid

data class Cover(
    val id: String,
    val desc: String,
    val videos: List<Video>?,
    val references: List<Reference>?
)
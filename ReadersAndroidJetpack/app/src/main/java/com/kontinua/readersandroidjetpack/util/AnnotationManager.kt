package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AnnotationManager {
    var scribbleEnabled by mutableStateOf(false)
        private set

    init {
        scribbleEnabled = false
    }

    fun toggleScribble(boolean: Boolean){
        scribbleEnabled = boolean
    }
}
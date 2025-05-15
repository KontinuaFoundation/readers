package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AnnotationManager {
    var scribbleEnabled by mutableStateOf(false)
        private set

    var penEnabled by mutableStateOf(false)
        private set

    var highlightEnabled by mutableStateOf(false)
        private set

    var eraseEnabled by mutableStateOf(false)
        private set

    val annotationsEnabled: Boolean
        get() = scribbleEnabled ||
            penEnabled ||
            highlightEnabled ||
            eraseEnabled

    init {
        scribbleEnabled = false
        penEnabled = false
        eraseEnabled = false
        highlightEnabled = false
    }

    fun toggleScribble(boolean: Boolean) {
        scribbleEnabled = boolean
        if (!boolean) {
            penEnabled = false
            highlightEnabled = false
            eraseEnabled = false
        }
    }

    fun togglePen(boolean: Boolean) {
        penEnabled = boolean
        eraseEnabled = false
        highlightEnabled = false
    }

    fun toggleHighlight(boolean: Boolean) {
        highlightEnabled = boolean
        penEnabled = false
        eraseEnabled = false
    }

    fun toggleErase(boolean: Boolean) {
        eraseEnabled = boolean
        penEnabled = false
        highlightEnabled = false
    }
}

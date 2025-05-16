package com.kontinua.readersandroidjetpack.util
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

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

    var currentPenColor by mutableStateOf(Color.Black)
        private set

    var currentHighlightColor by mutableStateOf(Color.Yellow)
        private set

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

    fun setPenColor(color: Color) {
        currentPenColor = color
        penEnabled = true
        highlightEnabled = false
        eraseEnabled = false
        scribbleEnabled = true
    }

    fun toggleHighlight(boolean: Boolean) {
        highlightEnabled = boolean
        penEnabled = false
        eraseEnabled = false
    }

    fun setHighlightColor(color: Color) {
        currentHighlightColor = color
        penEnabled = false
        highlightEnabled = true
        eraseEnabled = false
        scribbleEnabled = true
    }

    fun toggleErase(boolean: Boolean) {
        eraseEnabled = boolean
        penEnabled = false
        highlightEnabled = false
    }
}

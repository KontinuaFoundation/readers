package com.kontinua.readersandroidjetpack.util
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AnnotationMode {
    NONE,
    PEN,
    HIGHLIGHT,
    ERASE,
    TEXT
}

class AnnotationManager {
    var mode: AnnotationMode = AnnotationMode.NONE

    val annotationsEnabled: AnnotationMode
        get() = mode

    var currentPenColor by mutableStateOf(Color.Black)
        private set

    var currentHighlightColor by mutableStateOf(Color.Yellow)
        private set

    fun toggleScribble(boolean: Boolean) {
        if (!boolean) {
            mode = AnnotationMode.NONE
        }
    }

    fun togglePen(boolean: Boolean) {
        mode = AnnotationMode.PEN
        toggleScribble(boolean)
    }

    fun setPenColor(color: Color) {
        currentPenColor = color
        togglePen(true)
    }

    fun toggleHighlight(boolean: Boolean) {
        mode = AnnotationMode.HIGHLIGHT
        toggleScribble(boolean)
    }

    fun setHighlightColor(color: Color) {
        currentHighlightColor = color
        toggleHighlight(true)
    }

    fun toggleErase(boolean: Boolean) {
        mode = AnnotationMode.ERASE
        toggleScribble(boolean)
    }

    fun toggleText(boolean: Boolean) {
        mode = AnnotationMode.TEXT
        toggleScribble(boolean)
    }
}

package com.kontinua.readersandroidjetpack.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Color
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingStore
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.OffsetSerializable
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.TextAnnotation

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
  
  var clearEnabled by mutableStateOf(false)
        private set

    var isFocused by mutableStateOf(false)
        private set

    var currentPenColor by mutableStateOf(Color.Black)
        private set

    var currentHighlightColor by mutableStateOf(Color.Yellow)
        private set
  
    init{
      isFocused = false
      clearEnabled = false
    }
  
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

    fun toggleClear(boolean: Boolean) {
        clearEnabled = boolean
    }

    fun toggleFocus(boolean: Boolean) {
        isFocused = boolean
    }

    fun addTextAnnotation(annotation: TextAnnotation) {
        textAnnotations.add(annotation)
    }

    fun updateText(
        id: String,
        newPos: OffsetSerializable? = null,
        newSize: OffsetSerializable? = null,
        newText: String? = null
    ) {
        val index = textAnnotations.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = textAnnotations[index].copy(
                position = newPos ?: textAnnotations[index].position,
                size = newSize ?: textAnnotations[index].size,
                text = newText ?: textAnnotations[index].text
            )
            textAnnotations[index] = updated
        }
    }

    fun removeText(id: String) {
        textAnnotations.removeAll { it.id == id }
    }

    fun getText(context: Context, workbookId: String, page: Int) {
        textAnnotations = DrawingStore.getTextAnnotations(context, workbookId, page).toMutableStateList()
    }
}

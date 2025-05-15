package com.kontinua.readersandroidjetpack.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingStore
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.OffsetSerializable
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.TextAnnotation

class AnnotationManager {
    var scribbleEnabled by mutableStateOf(false)
        private set

    var penEnabled by mutableStateOf(false)
        private set

    var highlightEnabled by mutableStateOf(false)
        private set

    var eraseEnabled by mutableStateOf(false)
        private set

    var textEnabled by mutableStateOf(false)
        private set

    var textAnnotations = mutableStateListOf<TextAnnotation>()

    init {
        scribbleEnabled = false
        penEnabled = false
        eraseEnabled = false
        highlightEnabled = false
        textEnabled = false
    }

    fun toggleScribble(boolean: Boolean) {
        scribbleEnabled = boolean
        if (!boolean) {
            penEnabled = false
            highlightEnabled = false
            eraseEnabled = false
            textEnabled = false
        }
    }

    fun togglePen(boolean: Boolean) {
        penEnabled = boolean
        eraseEnabled = false
        highlightEnabled = false
        textEnabled = false
    }

    fun toggleHighlight(boolean: Boolean) {
        highlightEnabled = boolean
        penEnabled = false
        eraseEnabled = false
        textEnabled = false
    }

    fun toggleErase(boolean: Boolean) {
        eraseEnabled = boolean
        penEnabled = false
        highlightEnabled = false
        textEnabled = false
    }

    fun toggleText(boolean: Boolean) {
        textEnabled = boolean
        penEnabled = false
        highlightEnabled = false
        eraseEnabled = false
    }

    fun addTextAnnotation(annotation: TextAnnotation) {
        textAnnotations.add(annotation)
    }

    fun updateText(id: String, newPos: OffsetSerializable? = null, newSize: OffsetSerializable? = null, newText: String? = null) {
        val index = textAnnotations.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = textAnnotations[index]
            textAnnotations[index] = old.copy(
                position = newPos ?: textAnnotations[index].position,
                size = newSize ?: textAnnotations[index].size,
                text = newText ?: textAnnotations[index].text
            )
        }
    }

    fun removeText(id: String) {
        textAnnotations.removeAll { it.id == id }
    }

    fun getText(context: Context, workbookId: String, page: Int){
        textAnnotations = DrawingStore.getTextAnnotations(context, workbookId, page).toMutableStateList()
    }
}

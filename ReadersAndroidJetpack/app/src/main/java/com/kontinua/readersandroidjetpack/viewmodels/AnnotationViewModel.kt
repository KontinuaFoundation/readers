package com.kontinua.readersandroidjetpack.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel

class AnnotationViewModel : ViewModel() {
    data class DrawingPath(val points: List<Offset>)

    object DrawingStore {
        // Map: Workbook ID → Page Number → List of Drawings
        val drawings: MutableMap<String, MutableMap<Int, MutableList<DrawingPath>>> = mutableMapOf()

        fun getPaths(workbookId: String, page: Int): List<DrawingPath> {
            return drawings[workbookId]?.get(page) ?: emptyList()
        }

        fun addPath(workbookId: String, page: Int, path: DrawingPath) {
            val pages = drawings.getOrPut(workbookId) { mutableMapOf() }
            val pathList = pages.getOrPut(page) { mutableListOf() }
            pathList.add(path)
        }
    }
}
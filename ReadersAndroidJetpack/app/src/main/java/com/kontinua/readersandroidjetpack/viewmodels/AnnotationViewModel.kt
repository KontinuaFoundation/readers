package com.kontinua.readersandroidjetpack.viewmodels

import android.content.Context
import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


class AnnotationViewModel : androidx.lifecycle.ViewModel() {
    data class DrawingPath(val points: List<Offset>, val isHighlight: Boolean = false)

    @OptIn(kotlinx.serialization.InternalSerializationApi::class)
    @Serializable
    data class OffsetSerializable(val x: Float, val y: Float)

    @OptIn(kotlinx.serialization.InternalSerializationApi::class)
    @Serializable
    data class DrawingPathSerializable(
        val points: List<OffsetSerializable>,
        val isHighlight: Boolean = false
    )

    object DrawingStore {
        private const val DIR_NAME = "annotations"

        fun addPath(context: Context, workbookId: String, page: Int, path: DrawingPath) {
            val current = getPaths(context, workbookId, page).toMutableList()
            current.add(path)
            val serializableList = current.map { drawingPath ->
                DrawingPathSerializable(
                    drawingPath.points.map { offset ->
                        OffsetSerializable(offset.x, offset.y)
                    },
                    drawingPath.isHighlight
                )
            }
            savePaths(context, workbookId, page, serializableList)
        }

        fun getPaths(context: Context, workbookId: String, page: Int): List<DrawingPath> {
            return try {
                val file = getFile(context, workbookId, page)
                if (!file.exists()) return emptyList()
                val json = file.readText()
                Json.decodeFromString<List<DrawingPathSerializable>>(json)
                    .map { DrawingPath(it.points.map { pt -> Offset(pt.x, pt.y) }, isHighlight = it.isHighlight) }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        fun savePaths(context: Context, workbookId: String, page: Int, paths: List<DrawingPathSerializable>) {
            try {
                val file = getFile(context, workbookId, page)
                file.parentFile?.mkdirs()
                file.writeText(Json.encodeToString(paths))
                println("Saved ${paths.size} paths to ${file.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
                println("Failed to save paths!")
            }
        }

        private fun getFile(context: Context, workbookId: String, page: Int): File {
            val dir = File(context.filesDir, DIR_NAME)
            return File(dir, "${workbookId}_page_$page.json")
        }
    }
}

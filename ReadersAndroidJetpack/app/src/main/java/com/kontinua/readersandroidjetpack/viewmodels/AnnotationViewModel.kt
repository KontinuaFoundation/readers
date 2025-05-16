package com.kontinua.readersandroidjetpack.viewmodels
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnnotationViewModel : androidx.lifecycle.ViewModel() {
    // made default black
    data class DrawingPath(
        val points: List<Offset>,
        val isHighlight: Boolean = false,
        val color: Color = Color.Black
    )

    @Serializable
    data class OffsetSerializable(val x: Float, val y: Float)

    @Serializable
    data class DrawingPathSerializable(
        val points: List<OffsetSerializable>,
        val isHighlight: Boolean = false,
        val colorValue: Long = Color.Black.toArgb().toLong()
    )

    @OptIn(kotlinx.serialization.InternalSerializationApi::class)
    @Serializable
    data class TextAnnotation(
        val id: String = UUID.randomUUID().toString(), // uniquely identify each textbox
        val text: String,
        val position: OffsetSerializable,
        val size: OffsetSerializable,
        val fontSize: Float = 16f,
        val colorHex: String = "#000000"
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
                    drawingPath.isHighlight,
                    colorValue = drawingPath.color.toArgb().toLong()
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
                    .map {
                        DrawingPath(
                            it.points.map { pt -> Offset(pt.x, pt.y) },
                            isHighlight = it.isHighlight,
                            color = Color(it.colorValue.toInt())
                        )
                    }
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

        fun saveTextAnnotations(context: Context, workbookId: String, page: Int, annotations: List<TextAnnotation>) {
            val file = File(context.filesDir, "text-$workbookId-$page.json")
            file.writeText(Json.encodeToString(annotations))
        }

        fun getTextAnnotations(context: Context, workbookId: String, page: Int): List<TextAnnotation> {
            val file = File(context.filesDir, "text-$workbookId-$page.json")
            return if (file.exists()) {
                Json.decodeFromString(file.readText())
            } else {
                emptyList()
            }
        }
    }
}

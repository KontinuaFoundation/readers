package com.kontinua.readersandroidjetpack.util

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPath
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayOutputStream
import java.io.File

class AnnotationManager {
    var scribbleEnabled by mutableStateOf(false)
        private set

    var pageWidth: Int
        private set

    var pageHeight: Int
        private set

    init {
        scribbleEnabled = false
        pageWidth = 1080
        pageHeight = 1920
    }

    fun setWidth(width: Int){
        pageWidth = width
    }

    fun setHeight(height: Int){
        pageHeight = height
    }

    fun toggleScribble(){
        scribbleEnabled = !scribbleEnabled
    }

    fun createBitmapFromPaths(
        paths: List<DrawingPath>,
        strokeColor: Int = Color.Black.toArgb(),
        strokeWidth: Float = 5f
    ): Bitmap {
        val bitmap = createBitmap(pageWidth, pageHeight)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = strokeColor
            style = Paint.Style.STROKE
            isAntiAlias = true
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (drawing in paths) {
            val path = android.graphics.Path()
            drawing.points.firstOrNull()?.let {
                path.moveTo(it.x, it.y)
                for (point in drawing.points.drop(1)) {
                    path.lineTo(point.x, point.y)
                }
            }
            canvas.drawPath(path, paint)
        }


        return bitmap
    }

    fun embedDrawingInPdf(
        originalPdf: File,
        pageNumber: Int, // zero-based
        bitmap: Bitmap,
        outputPdf: File
    ) {
        val document = PDDocument.load(originalPdf)
        val page = document.getPage(pageNumber)

        val stream = PDImageXObject.createFromByteArray(
            document,
            ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
            }.toByteArray(),
            "drawing"
        )

        val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)
        contentStream.drawImage(stream, 0f, 0f, page.mediaBox.width, page.mediaBox.height)
        contentStream.close()

        document.save(outputPdf)
        document.close()
    }
}
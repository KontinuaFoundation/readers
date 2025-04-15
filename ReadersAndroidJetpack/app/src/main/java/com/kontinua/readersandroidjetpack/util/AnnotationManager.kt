package com.kontinua.readersandroidjetpack.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPath
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
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

    fun setWidth(int: Int){
        pageWidth = int
    }

    fun setHeight(int: Int){
        pageWidth = int
    }

    fun toggleScribble(boolean: Boolean){
        scribbleEnabled = boolean
    }

    object PDFAnnotationEmbed {
        fun embedAnnotationsIntoPDF(
            originalPdfFile: File,
            drawings: Map<Int, List<DrawingPath>>,
            viewWidth: Int,
            viewHeight: Int,
            outputFile: File
        ): Boolean {
            try {
                val document = PDDocument.load(originalPdfFile)

                drawings.forEach { (pageIndex, paths) ->
                    if (pageIndex >= document.numberOfPages) return@forEach
                    val page = document.getPage(pageIndex)
                    val mediaBox: PDRectangle = page.mediaBox
                    println("Paths: $drawings")

                    val contentStream = PDPageContentStream(
                        document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true
                    )

                    contentStream.setStrokingColor(0f, 0f, 0f) // Black lines
                    contentStream.setLineWidth(2f)

                    paths.forEach { path ->
                        if (path.points.isEmpty()) return@forEach
                        val (startX, startY) = convertToPDFCoordinates(
                            path.points.first(), viewWidth, viewHeight, mediaBox.width, mediaBox.height
                        )
                        contentStream.moveTo(startX, startY)

                        for (point in path.points.drop(1)) {
                            val (x, y) = convertToPDFCoordinates(
                                point, viewWidth, viewHeight, mediaBox.width, mediaBox.height
                            )
                            contentStream.lineTo(x, y)
                        }

                        contentStream.stroke()
                    }

                    contentStream.close()
                }

                document.save(outputFile)
                document.close()
                return true

            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        private fun convertToPDFCoordinates(
            point: androidx.compose.ui.geometry.Offset,
            viewWidth: Int,
            viewHeight: Int,
            pdfWidth: Float,
            pdfHeight: Float
        ): Pair<Float, Float> {
            val scaleX = pdfWidth / viewWidth
            val scaleY = pdfHeight / viewHeight
            return Pair(point.x * scaleX, pdfHeight - (point.y * scaleY)) // Invert Y
        }
    }
}
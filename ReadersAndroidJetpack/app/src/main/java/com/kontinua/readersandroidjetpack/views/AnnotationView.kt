package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPath
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingStore
import java.io.File

@Composable
fun DrawingCanvas(workbookId: String, page: Int, annotationManager: AnnotationManager) {
    val savedPaths = remember { DrawingStore.getPaths(workbookId, page).toMutableStateList() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> currentPath = listOf(offset) },
                    onDrag = { change, _ -> currentPath += change.position },
                    onDragEnd = {
                        val newPath = DrawingPath(currentPath)
                        savedPaths.add(newPath)
                        DrawingStore.addPath(workbookId, page, newPath)
                        currentPath = emptyList()
                    }
                )
            }
    ) {
        savedPaths.forEach { drawPathLine(it.points) }
        drawPathLine(currentPath)
    }
}

private fun DrawScope.drawPathLine(points: List<Offset>) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}



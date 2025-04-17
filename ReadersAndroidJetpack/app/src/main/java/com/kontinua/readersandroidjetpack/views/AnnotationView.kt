package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPath
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingStore

@Composable
fun DrawingCanvas(workbookId: String, page: Int,
                  annotationManager: AnnotationManager, offset: Float) {
    var savedPaths = remember(workbookId, page) {
        DrawingStore.getPaths(workbookId, page).toMutableStateList()
    }
    var currentPath by remember(workbookId, page) {
        mutableStateOf<List<Offset>>(emptyList())
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }

    var swipeSpeedMultiplier = 0f
    var offsetX = -(offset * screenWidthPx * swipeSpeedMultiplier).toInt()

    // Load drawing paths
    LaunchedEffect(workbookId, page) {
        val paths = DrawingStore.getPaths(workbookId, page)
        savedPaths = paths.toMutableStateList()
    }

    val gestureModifier = if (annotationManager.scribbleEnabled) {
        Modifier.pointerInput(workbookId, page) {
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
    } else {
        Modifier
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX, 0)  }
            .then(gestureModifier)
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

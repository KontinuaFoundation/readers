package com.kontinua.readersandroidjetpack.views

import android.content.Context
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPath
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPathSerializable
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingStore
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.OffsetSerializable

@Composable
fun DrawingCanvas(
    workbookId: String,
    page: Int,
    annotationManager: AnnotationManager,
    context: Context,
    zoom: Float,
    pan: Offset
) {
    var savedPaths by remember(workbookId, page) {
        mutableStateOf(DrawingStore.getPaths(context, workbookId, page).toMutableStateList())
    }
    var currentPath by remember(workbookId, page) {
        mutableStateOf<List<Offset>>(emptyList())
    }

    // Load drawing paths
    LaunchedEffect(workbookId, page) {
        val paths = DrawingStore.getPaths(context, workbookId, page)
        savedPaths = paths.toMutableStateList()
    }

    val gestureModifier = if (annotationManager.scribbleEnabled) {
        Modifier.pointerInput(workbookId, page) {
            detectDragGestures(
                onDragStart = { offset ->
                    val normalized = Offset(offset.x / size.width, offset.y / size.height)
                    if (!annotationManager.eraseEnabled) {
                        currentPath = listOf(normalized)
                    }
                },
                onDrag = { change, _ ->
                    val normalized = Offset(change.position.x / size.width, change.position.y / size.height)
                    if (annotationManager.eraseEnabled) {
                        val eraseThreshold = 0.02f
                        val touch = normalized
                        val removed = savedPaths.removeAll { path ->
                            path.points.any { pt -> (pt - touch).getDistance() < eraseThreshold }
                        }
                        if (removed) {
                            val serializableList = savedPaths.map { path ->
                                DrawingPathSerializable(
                                    points = path.points.map { pt -> OffsetSerializable(pt.x, pt.y) },
                                    isHighlight = path.isHighlight,
                                    colorValue = path.color.toArgb().toLong()
                                )
                            }
                            DrawingStore.savePaths(context, workbookId, page, serializableList)
                        }
                    } else {
                        currentPath += normalized
                    }
                },
                onDragEnd = {
                    if (!annotationManager.eraseEnabled && currentPath.isNotEmpty()) {
                        val newPath = DrawingPath(
                            currentPath,
                            isHighlight = annotationManager.highlightEnabled,
                            color = if (annotationManager.highlightEnabled) {
                                annotationManager.currentHighlightColor
                            } else {
                                annotationManager.currentPenColor
                            }
                        )
                        savedPaths.add(newPath)
                        DrawingStore.addPath(context, workbookId, page, newPath)
                        currentPath = emptyList()
                    }
                }
            )
        }
    } else {
        // allows gestures to pass through when annotations are disabled
        Modifier
    }

    // swiping the page continuously builds the offset (page 1: 0-1600; page 2: 1600-3200)
    // panX corrects offset to 0 when zooming to fix location of annotations
    // page starts at -45 so panY corrects while also accounting for zoom
    val panX = 1600 * zoom * page
    val panY = 45 * zoom
    var newOffsetX = (panX - pan.x).toInt()
    var newOffsetY = (-pan.y - panY).toInt()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(newOffsetX, newOffsetY) }
            .then(gestureModifier)
    ) {
        val pageWidth = size.width
        val pageHeight = size.height

        savedPaths.forEach {
            drawPathLine(it, pageWidth, pageHeight, zoom)
        }
        drawPathLine(
            DrawingPath(
                currentPath,
                isHighlight = annotationManager.highlightEnabled,
                color = if (annotationManager.highlightEnabled) annotationManager.currentHighlightColor else annotationManager.currentPenColor
            ),
            pageWidth,
            pageHeight,
            zoom
        )
    }
}

private fun DrawScope.drawPathLine(
    path: DrawingPath,
    pageWidth: Float,
    pageHeight: Float,
    zoom: Float
) {
    val points = path.points
    val highlight = path.isHighlight
//    val pathColor = path.color
    val pathColor = if (path.isHighlight) path.color.copy(alpha = 0.4f) else path.color
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(
            (points.first().x * pageWidth) * zoom,
            (points.first().y * pageHeight) * zoom
        )
        for (i in 1 until points.size) {
            lineTo(
                (points[i].x * pageWidth) * zoom,
                (points[i].y * pageHeight) * zoom
            )
        }
    }
    drawPath(
        path = path,
        color = pathColor,
        style = Stroke(
            width = if (highlight) 20f * zoom else 5f * zoom,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

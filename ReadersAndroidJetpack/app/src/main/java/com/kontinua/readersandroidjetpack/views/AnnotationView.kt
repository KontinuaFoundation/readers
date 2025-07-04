package com.kontinua.readersandroidjetpack.views

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.AnnotationMode
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPath
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingPathSerializable
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.DrawingStore
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.OffsetSerializable
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.TextAnnotation

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
    var deleteText by remember { mutableStateOf(false) }
    val canvasSize = remember { mutableStateOf(Size.Zero) }
    val textToDelete = remember { mutableStateOf<TextAnnotation?>(null) }
    val focusManager = LocalFocusManager.current
    var hasAnnotations by mutableStateOf(false)

    // Load drawing paths
    LaunchedEffect(workbookId, page) {
        val paths = DrawingStore.getPaths(context, workbookId, page)
        savedPaths = paths.toMutableStateList()
        annotationManager.getText(context, workbookId, page)
        hasAnnotations = annotationManager.textAnnotations.isNotEmpty() || savedPaths.isNotEmpty()
    }

    val gestureModifier = if (annotationManager.mode == AnnotationMode.PEN ||
        annotationManager.mode == AnnotationMode.HIGHLIGHT ||
        annotationManager.mode == AnnotationMode.ERASE ||
        annotationManager.mode == AnnotationMode.TEXT
    ) {
        Modifier.pointerInput(
            workbookId,
            page,
            annotationManager.mode,
            annotationManager.textAnnotations,
            annotationManager.isFocused,
            hasAnnotations
        ) {
            when (annotationManager.mode) {
                AnnotationMode.TEXT -> detectTapGestures(
                    onTap = { offset ->
                        if (annotationManager.isFocused) {
                            focusManager.clearFocus()
                            annotationManager.toggleFocus(false)
                        } else {
                            val normalized = OffsetSerializable(offset.x / size.width, offset.y / size.height)
                            val newAnnotation = TextAnnotation(
                                text = "",
                                position = normalized,
                                size = OffsetSerializable(0.3f, 0.1f)
                            )
                            annotationManager.addTextAnnotation(newAnnotation)
                            DrawingStore.saveTextAnnotations(
                                context,
                                workbookId,
                                page,
                                annotationManager.textAnnotations
                            )
                        }
                    }
                )
                AnnotationMode.PEN, AnnotationMode.HIGHLIGHT, AnnotationMode.ERASE -> detectDragGestures(
                    onDragStart = { offset ->
                        val normalized = Offset(offset.x / size.width, offset.y / size.height)
                        if (annotationManager.mode != AnnotationMode.ERASE) {
                            currentPath = listOf(normalized)
                        }
                    },
                    onDrag = { change, _ ->
                        val normalized = Offset(change.position.x / size.width, change.position.y / size.height)
                        if (annotationManager.mode == AnnotationMode.ERASE) {
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
                        } else if (annotationManager.mode == AnnotationMode.PEN ||
                            annotationManager.mode == AnnotationMode.HIGHLIGHT
                        ) {
                            currentPath += normalized
                        }
                    },
                    onDragEnd = {
                        if (annotationManager.mode != AnnotationMode.ERASE && currentPath.isNotEmpty()) {
                            val newPath = DrawingPath(
                                currentPath,
                                isHighlight = annotationManager.mode == AnnotationMode.HIGHLIGHT,
                                color = if (annotationManager.mode == AnnotationMode.HIGHLIGHT) {
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
                else ->
                    if (annotationManager.isFocused) {
                        focusManager.clearFocus()
                        annotationManager.toggleFocus(false)
                    }
            }
        }
    } else {
        // allows gestures to pass through when annotations are disabled
        if (annotationManager.isFocused) {
            focusManager.clearFocus()
            annotationManager.toggleFocus(false)
        }
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
        canvasSize.value = Size(size.width.toFloat(), size.height.toFloat())

        if (annotationManager.mode != AnnotationMode.HIDDEN) {
            savedPaths.forEach {
                drawPathLine(it, pageWidth, pageHeight, zoom)
            }
            drawPathLine(
                DrawingPath(
                    currentPath,
                    isHighlight = annotationManager.mode == AnnotationMode.HIGHLIGHT,
                    color = if (annotationManager.mode == AnnotationMode.HIGHLIGHT) {
                        annotationManager.currentHighlightColor
                    } else {
                        annotationManager.currentPenColor
                    }
                ),
                pageWidth,
                pageHeight,
                zoom
            )
        }
    }
    hasAnnotations = annotationManager.textAnnotations.isNotEmpty() || savedPaths.isNotEmpty()
    if (annotationManager.mode != AnnotationMode.HIDDEN) {
        for (i in annotationManager.textAnnotations.indices) {
            val annotation = annotationManager.textAnnotations[i]
            MovableTextBox(
                annotation = annotation,
                zoom = zoom,
                pan = Offset(panX - pan.x, -pan.y - panY),
                canvasSize = canvasSize.value,
                onMove = { newPos ->
                    annotationManager.updateText(
                        annotation.id,
                        newPos = OffsetSerializable(newPos.x, newPos.y)
                    )
                    DrawingStore.saveTextAnnotations(
                        context,
                        workbookId,
                        page,
                        annotationManager.textAnnotations
                    )
                },
                onEdit = { newText ->
                    annotationManager.updateText(annotation.id, newText = newText)
                    DrawingStore.saveTextAnnotations(context, workbookId, page, annotationManager.textAnnotations)
                },
                onResize = { newSize ->
                    annotationManager.updateText(annotation.id, newSize = OffsetSerializable(newSize.x, newSize.y))
                    DrawingStore.saveTextAnnotations(context, workbookId, page, annotationManager.textAnnotations)
                },
                onDelete = {
                    textToDelete.value = annotation
                    deleteText = true
                },
                onFocusChange = { isFocused ->
                    annotationManager.toggleFocus(true)
                }
            )
        }
    }
    if (hasAnnotations || annotationManager.mode == AnnotationMode.HIDDEN) {
        IconButton(
            onClick = {
                if (annotationManager.mode == AnnotationMode.HIDDEN) {
                    annotationManager.mode = annotationManager.hideMode
                    annotationManager.hideMode = AnnotationMode.NONE
                } else {
                    annotationManager.hideMode = annotationManager.mode
                    annotationManager.toggleTool(AnnotationMode.HIDDEN)
                }
            },
            modifier = Modifier
                .padding(16.dp)
                .size(48.dp)
                .offset(0.dp, 1000.dp)
        ) {
            Icon(
                imageVector = if (annotationManager.mode != AnnotationMode.HIDDEN) {
                    Icons.Default.Visibility
                } else {
                    Icons.Default.VisibilityOff
                },
                contentDescription = "Toggle Textbox Visibility",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    if (deleteText && textToDelete.value != null) {
        ConfirmDeleteDialog(
            onConfirm = {
                annotationManager.removeText(textToDelete.value!!.id)
                DrawingStore.saveTextAnnotations(context, workbookId, page, annotationManager.textAnnotations)
                deleteText = false
                textToDelete.value = null
            },
            onDismiss = {
                deleteText = false
                textToDelete.value = null
            }
        )
    }
    if (annotationManager.mode == AnnotationMode.CLEAR) {
        ConfirmClearDialog(
            onConfirm = {
                savedPaths.clear()
                annotationManager.textAnnotations.clear()
                val serializableList = savedPaths.map { path ->
                    DrawingPathSerializable(
                        points = path.points.map { pt -> OffsetSerializable(pt.x, pt.y) },
                        isHighlight = path.isHighlight
                    )
                }
                DrawingStore.savePaths(context, workbookId, page, serializableList)
                DrawingStore.saveTextAnnotations(context, workbookId, page, annotationManager.textAnnotations)
                annotationManager.toggleTool(annotationManager.prevMode)
            },
            onDismiss = {
                annotationManager.toggleTool(annotationManager.prevMode)
            }
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

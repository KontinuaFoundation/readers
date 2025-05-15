package com.kontinua.readersandroidjetpack.views

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.kontinua.readersandroidjetpack.util.AnnotationManager
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
    var showTextDialog by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf(false) }
    val canvasSize = remember { mutableStateOf(Size.Zero) }
    val textToEdit = remember { mutableStateOf<TextAnnotation?>(null) }
    val textToDelete = remember { mutableStateOf<TextAnnotation?>(null) }

    // Load drawing paths
    LaunchedEffect(workbookId, page) {
        val paths = DrawingStore.getPaths(context, workbookId, page)
        savedPaths = paths.toMutableStateList()
        annotationManager.getText(context, workbookId, page)
    }

    val gestureModifier = if (annotationManager.scribbleEnabled) {
        Modifier.pointerInput(
            workbookId, page,
            annotationManager.textEnabled,
            annotationManager.penEnabled,
            annotationManager.highlightEnabled,
            annotationManager.eraseEnabled) {
            if (annotationManager.textEnabled){
                detectTapGestures(
                    onTap = { offset ->
                        val normalized = OffsetSerializable(offset.x / size.width, offset.y / size.height)
                        val newAnnotation = TextAnnotation(
                            text = "Edit Text",
                            position = normalized,
                            size = OffsetSerializable(0.3f, 0.075f)
                        )
                        annotationManager.addTextAnnotation(newAnnotation)
                        DrawingStore.saveTextAnnotations(
                            context, workbookId, page,
                            annotationManager.textAnnotations
                        )
                    }
                )
            } else {
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
                                        isHighlight = path.isHighlight
                                    )
                                }
                                DrawingStore.savePaths(context, workbookId, page, serializableList)
                            }
                        } else if (annotationManager.penEnabled || annotationManager.highlightEnabled) {
                            currentPath += normalized
                        }
                    },
                    onDragEnd = {
                        if (!annotationManager.eraseEnabled && currentPath.isNotEmpty()) {
                            val newPath = DrawingPath(currentPath, isHighlight = annotationManager.highlightEnabled)
                            savedPaths.add(newPath)
                            DrawingStore.addPath(context, workbookId, page, newPath)
                            currentPath = emptyList()
                        }
                    }
                )
            }
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
        canvasSize.value = Size(size.width.toFloat(), size.height.toFloat())

        savedPaths.forEach {
            drawPathLine(it, pageWidth, pageHeight, zoom)
        }
        drawPathLine(
            DrawingPath(currentPath, isHighlight = annotationManager.highlightEnabled),
            pageWidth,
            pageHeight,
            zoom
        )
    }
    annotationManager.textAnnotations.toList().forEach { annotation ->
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
                    context, workbookId, page,
                    annotationManager.textAnnotations
                )
            },
            onEdit = {
                textToEdit.value = annotation
                showTextDialog = true
            },
            onResize = { newSize -> /* update size */ },
            onDelete = {
                textToDelete.value = annotation
                deleteText = true
            }
        )
    }
    if (showTextDialog && textToEdit.value != null) {
        var text by remember { mutableStateOf(textToEdit.value!!.text) }

        AlertDialog(
            onDismissRequest = {
                showTextDialog = false
                textToEdit.value = null
            },
            title = { Text("Edit Text") },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    annotationManager.updateText(
                        textToEdit.value!!.id,
                        newText = text
                    )
                    DrawingStore.saveTextAnnotations(
                        context, workbookId, page,
                        annotationManager.textAnnotations
                    )
                    showTextDialog = false
                    textToEdit.value = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTextDialog = false
                    textToEdit.value = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (deleteText && textToDelete.value != null){
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
}

private fun DrawScope.drawPathLine(
    path: DrawingPath,
    pageWidth: Float,
    pageHeight: Float,
    zoom: Float
) {
    val points = path.points
    val highlight = path.isHighlight
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
        color = if (highlight) Color.Yellow.copy(alpha = 0.4f) else Color.Black,
        style = Stroke(
            width = if (highlight) 20f * zoom else 5f * zoom,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kontinua.readersandroidjetpack.viewmodels.AnnotationViewModel.TextAnnotation

@Composable
fun MovableTextBox(
    annotation: TextAnnotation,
    zoom: Float,
    pan: Offset,
    canvasSize: Size,
    onMove: (Offset) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onResize: (Offset) -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    val position = Offset(annotation.position.x, annotation.position.y)
    val x = position.x * canvasSize.width * zoom + pan.x
    val y = position.y * canvasSize.height * zoom + pan.y
    val width = annotation.size.x * canvasSize.width * zoom
    val height = annotation.size.y * canvasSize.height * zoom
    val localDensity = LocalDensity.current
    var editingText by remember { mutableStateOf(annotation.text) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChange(isFocused)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(x.toInt(), y.toInt()) }
            .size(with(localDensity) { width.toDp() }, with(localDensity) { height.toDp() })
            .background(Color.White.copy(alpha = 0.5f))
            .border(1.dp, Color.Black)
    ) {
        // Move handle (top-left)
        Box(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.TopStart)
                .pointerInput(annotation.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = position.x + (dragAmount.x / (canvasSize.width * zoom))
                        val newY = position.y + (dragAmount.y / (canvasSize.height * zoom))
                        onMove(Offset(newX, newY))
                    }
                }
                .offset((-20).dp, (-20).dp)
                .background(Color.DarkGray.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.OpenWith, contentDescription = "Move", tint = Color.White)
        }

        // Resize handle (bottom-right)
        Box(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.BottomEnd)
                .pointerInput(annotation.id + "-resize") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newWidth = (width + dragAmount.x) / (canvasSize.width * zoom)
                        val newHeight = (height + dragAmount.y) / (canvasSize.height * zoom)
                        onResize(
                            Offset(
                                newWidth.coerceAtLeast(0.05f),
                                newHeight.coerceAtLeast(0.02f)
                            )
                        )
                    }
                }
                .offset(20.dp, 20.dp)
                .background(Color.DarkGray.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AspectRatio, contentDescription = "Resize", tint = Color.White)
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.TopEnd)
                .pointerInput(annotation.id + "-tap") {
                    detectTapGestures(
                        onDoubleTap = {
                            onDelete()
                            println("hello")
                        }
                    )
                }
                .offset(20.dp, (-20).dp)
                .background(Color.Red.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Trash", tint = Color.White)
        }

        // Main text area (tap/edit + double-tap/delete)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp, end = 6.dp, start = 6.dp, bottom = 6.dp)
        ) {
            BasicTextField(
                value = editingText,
                onValueChange = {
                    editingText = it
                    onEdit(it)
                },
                textStyle = TextStyle.Default.copy(
                    fontSize = annotation.fontSize.sp * zoom,
                    color = Color.Black
                ),
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(remember { FocusRequester() })
                    .focusable()
            )
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Textbox?") },
        text = { Text("Are you sure you want to delete this textbox?") },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
            }) {
                Text("Delete", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmClearDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Screen?") },
        text = { Text("Are you sure you want to clear your screen?") },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
            }) {
                Text("Delete", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

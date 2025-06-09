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
import androidx.compose.runtime.rememberUpdatedState
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
    val currentAnnotation by rememberUpdatedState(annotation)
    val position = Offset(currentAnnotation.position.x, currentAnnotation.position.y)
    val x = position.x * canvasSize.width * zoom + pan.x
    val y = position.y * canvasSize.height * zoom + pan.y
    val xDp = with(LocalDensity.current) { x.toDp() }
    val yDp = with(LocalDensity.current) { y.toDp() }
    val width = currentAnnotation.size.x * canvasSize.width * zoom
    val height = currentAnnotation.size.y * canvasSize.height * zoom
    val localDensity = LocalDensity.current
    var editingText by remember { mutableStateOf(currentAnnotation.text) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChange(isFocused)
    }
    println("h: $height w: $width")

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .size(with(localDensity) { width.toDp() }, with(localDensity) { height.toDp() })
            .background(Color.White.copy(alpha = 0.5f))
            .border(1.dp, Color.Black)
    ) {
        // Move handle (top-left)
        Box(
            modifier = Modifier
                .size(25.dp)
                .offset((-20).dp, (-20).dp)
                .align(Alignment.TopStart)
                .background(Color.DarkGray.copy(alpha = 0.6f))
                .pointerInput(annotation.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = currentAnnotation.position.x + (dragAmount.x / (canvasSize.width * zoom))
                        val newY = currentAnnotation.position.y + (dragAmount.y / (canvasSize.height * zoom))
                        onMove(Offset(newX, newY))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.OpenWith,
                contentDescription = "Move",
                tint = Color.White,
                modifier = Modifier.fillMaxSize() // Make entire box respond to touch
            )
        }

        // Resize handle (bottom-right)
        Box(
            modifier = Modifier
                .size(25.dp)
                .offset(20.dp, 20.dp)
                .background(Color.DarkGray.copy(alpha = 0.6f))
                .align(Alignment.BottomEnd)
                .pointerInput(annotation.id + "-resize") {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaWidth = dragAmount.x / (canvasSize.width * zoom)
                        val deltaHeight = dragAmount.y / (canvasSize.height * zoom)

                        val newWidth = currentAnnotation.size.x + deltaWidth
                        val newHeight = currentAnnotation.size.y + deltaHeight
                        onResize(
                            Offset(
                                newWidth.coerceIn(0.05f, 0.5f),
                                newHeight.coerceIn(0.02f, 0.2f)
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = "Resize",
                tint = Color.White,
                modifier = Modifier.fillMaxSize() // Make entire box respond to touch
            )
        }

        Box(
            modifier = Modifier
                .size(25.dp)
                .offset(20.dp, (-20).dp)
                .background(Color.Red.copy(alpha = 0.6f))
                .align(Alignment.TopEnd)
                .pointerInput(annotation.id + "-tap") {
                    detectTapGestures(
                        onTap = {
                            onDelete()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Trash",
                tint = Color.White,
                modifier = Modifier.fillMaxSize() // Make entire box respond to touch
            )
        }

        // Main text area (tap/edit + double-tap/delete)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(annotation.id + "-text-doubletap") {
                    detectTapGestures(
                        onDoubleTap = {
                            onDelete()
                        }
                    )
                }
                .padding(10.dp)
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

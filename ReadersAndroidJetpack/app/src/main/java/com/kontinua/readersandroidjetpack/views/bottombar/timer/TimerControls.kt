package com.kontinua.readersandroidjetpack.views.bottombar.timer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimerControls(
    isRunning: Boolean,
    onPauseResume: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit
) {
    // TODO: icon colors must be changed to match iOS before deployment
    Row(modifier = Modifier.padding(end = 8.dp)) {
        IconButton(onClick = onPauseResume) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Resume"
            )
        }
        IconButton(onClick = onRestart) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Restart")
        }
        IconButton(onClick = onCancel) {
            Icon(imageVector = Icons.Filled.Cancel, contentDescription = "Cancel")
        }
    }
}

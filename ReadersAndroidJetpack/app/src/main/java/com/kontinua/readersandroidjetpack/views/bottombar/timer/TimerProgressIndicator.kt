package com.kontinua.readersandroidjetpack.views.bottombar.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import kotlinx.coroutines.delay

/**
 * A standalone timer progress indicator that can be used independently
 * from the timer controls.
 */
@Composable
fun TimerProgressIndicator(timerViewModel: TimerViewModel) {
    val isTimerRunning = timerViewModel.isTimerRunning
    val timerDuration = timerViewModel.timerDuration
    val timeLeftMillis = timerViewModel.timeLeftMillis

    val progress by animateFloatAsState(
        targetValue = if (timerDuration > 0) 1f - (timeLeftMillis.toFloat() / timerDuration) else 0f, // Invert progress
        label = "timerProgress"
    )

    LaunchedEffect(timeLeftMillis) {
        if (timeLeftMillis > 0) {
            delay(100)
        }
    }

    if (timeLeftMillis > 0) { // Only show the progress bar if time is left
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = if (isTimerRunning) Color.Green else Color.Yellow,
            trackColor = Color.LightGray,
        )
    }
}
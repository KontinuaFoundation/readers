package com.kontinua.readersandroidjetpack.views.bottombar.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel

@Composable
fun TimerProgressIndicator(timerViewModel: TimerViewModel) {
    // Get states from ViewModel
    val isTimerRunning = timerViewModel.isTimerRunning
    val timerDuration = timerViewModel.timerDuration
    val timeLeftMillis = timerViewModel.timeLeftMillis
    val isTimerFinished = timerViewModel.isTimerFinished
    val isVisible = timeLeftMillis > 0 || isTimerFinished

    val targetProgress = when {
        isTimerFinished -> 1f
        timerDuration > 0 -> 1f - (timeLeftMillis.toFloat() / timerDuration)
        else -> 0f // Default case (timerDuration is 0 or less)
    }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        label = "timerProgress"
    )

    val targetColor = when {
        isTimerFinished -> Color(0xFF19BA00)
        isTimerRunning -> Color.Red
        timeLeftMillis > 0 -> Color(0xFFFFDB33)
        else -> Color.Transparent
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        label = "timerColor"
    )

    if (isVisible) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = animatedColor,
            trackColor = Color.LightGray,
            strokeCap = StrokeCap.Round
        )
    }
}

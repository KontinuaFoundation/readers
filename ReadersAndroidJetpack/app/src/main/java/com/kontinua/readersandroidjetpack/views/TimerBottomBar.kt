// TimerBottomBar.kt
package com.kontinua.readersandroidjetpack.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel // Import TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerBottomBar(timerViewModel: TimerViewModel) { // Use TimerViewModel
    val isTimerRunning = timerViewModel.isTimerRunning
    val timerDuration = timerViewModel.timerDuration
    val timeLeftMillis = timerViewModel.timeLeftMillis

    val progress by animateFloatAsState(
        targetValue = if (timerDuration > 0) timeLeftMillis.toFloat() / timerDuration else 0f,
        label = "timerProgress"
    )

    Column {
        if (timeLeftMillis > 0) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = if (isTimerRunning) Color.Green else Color.Yellow,
            )
        }

        BottomAppBar(
            actions = {
                if (timeLeftMillis > 0) {
                    TimerControls(
                        isRunning = isTimerRunning,
                        onPauseResume = {
                            if (isTimerRunning) {
                                timerViewModel.pauseTimer() // Use timerViewModel
                            } else {
                                timerViewModel.resumeTimer() // Use timerViewModel
                            }
                        },
                        onRestart = {
                            timerViewModel.resetTimer() // Use timerViewModel
                            timerViewModel.startTimer()
                        },
                        onCancel = {
                            timerViewModel.cancelTimer() // Use timerViewModel
                        }
                    )
                }
            }
        )
    }
}
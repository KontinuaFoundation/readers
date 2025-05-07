package com.kontinua.readersandroidjetpack.views.bottombar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.ui.theme.buttons.FeedbackButton
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.views.bottombar.timer.TimerControls

/**
 * Extensible bottom app bar component that can house various action buttons.
 * Currently includes a feedback button and timer, but can be extended with additional functionality.
 *
 * To add new functionality:
 * 1. Uncomment placeholder buttons or add new ones
 * 2. Implement click handlers
 * 3. Create corresponding dialogs/screens if needed
 */

@Composable
fun BottomBarComponent(
    feedbackViewModel: FeedbackViewModel,
    timerViewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val isTimerRunning = timerViewModel.isTimerRunning
    val timeLeftMillis = timerViewModel.timeLeftMillis
    val isTimerFinished = timerViewModel.isTimerFinished
    val showTimerControls = timeLeftMillis > 0 || isTimerFinished

    BottomAppBar(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showTimerControls) {
                TimerControls(
                    isRunning = isTimerRunning,
                    onPauseResume = {
                        if (isTimerRunning) {
                            timerViewModel.pauseTimer()
                        } else {
                            timerViewModel.resumeTimer()
                        }
                    },
                    onRestart = {
                        timerViewModel.resetTimer()
                        timerViewModel.startTimer()
                    },
                    onCancel = {
                        timerViewModel.resetTimer()
                    }
                )
            }

            // Middle space for potential future items
            Spacer(modifier = Modifier.weight(1f))

            // Right-aligned actions
            FeedbackButton(
                onClick = { feedbackViewModel.showFeedbackForm() },
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}

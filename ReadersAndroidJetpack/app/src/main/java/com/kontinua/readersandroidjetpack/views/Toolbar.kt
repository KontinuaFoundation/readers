package com.kontinua.readersandroidjetpack.views

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(collectionViewModel: CollectionViewModel, timerViewModel: TimerViewModel) {
    var showMarkupMenu by remember { mutableStateOf(false) }
    var showResourcesMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }


    TopAppBar(
        title = { Text("") },
        colors = TopAppBarDefaults.topAppBarColors(
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = {
            TextButton(onClick = { showTimerMenu = true }) {
                Text("Timer")
            }
            DropdownMenu(
                expanded = showTimerMenu,
                onDismissRequest = { showTimerMenu = false }
            ) {
                DropdownMenuItem(text = { Text("15 Minutes") }, onClick = {
                    timerViewModel.setDurationAndReset(15 * 60 * 1000L) // Use timerViewModel
                    showTimerMenu = false
                })
                DropdownMenuItem(text = { Text("20 Minutes") }, onClick = {
                    timerViewModel.setDurationAndReset(20 * 60 * 1000L) // Use timerViewModel
                    showTimerMenu = false
                })
                DropdownMenuItem(text = { Text("25 Minutes") }, onClick = {
                    timerViewModel.setDurationAndReset(25 * 60 * 1000L) // Use timerViewModel
                    showTimerMenu = false
                })

            }
            // Markup Button (Text Button)
            TextButton(onClick = { showMarkupMenu = true }) {
                Text("Markup")
            }
            DropdownMenu(
                expanded = showMarkupMenu,
                onDismissRequest = { showMarkupMenu = false }
            ) {
                DropdownMenuItem(text = { Text("Pen") }, onClick = { /* TODO */ })
                DropdownMenuItem(text = { Text("Highlight") }, onClick = { /* TODO */ })
                DropdownMenuItem(text = { Text("Eraser") }, onClick = { /* TODO */ })
            }

            // Resources Button (Text Button)
            TextButton(onClick = { showResourcesMenu = true }) {
                Text("Digital Resources")
            }
            DropdownMenu(
                expanded = showResourcesMenu,
                onDismissRequest = { showResourcesMenu = false }
            ) {
                DropdownMenuItem(text = { Text("Article") }, onClick = { /* TODO */ })
                DropdownMenuItem(text = { Text("Video") }, onClick = { /* TODO */ })
            }
        }
    )
}






//package com.kontinua.readersandroidjetpack.views
//
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.absoluteOffset
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Cancel
//import androidx.compose.material.icons.filled.Pause
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material3.DropdownMenu
//import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.LinearProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableLongStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MyToolbar(collectionViewModel: CollectionViewModel) {
//    var showTimerMenu by remember { mutableStateOf(false) }
//    var showMarkupMenu by remember { mutableStateOf(false) }
//    var showResourcesMenu by remember { mutableStateOf(false) }
//
//    // Timer state
//    var isTimerRunning by remember { mutableStateOf(false) }
//    var timerDuration by remember { mutableLongStateOf(15 * 60 * 1000) } // Default: 15 minutes
//    var timeLeftMillis by remember { mutableLongStateOf(timerDuration) }
//    var timerJob by remember { mutableStateOf<Job?>(null) }
//    val progress by animateFloatAsState(
//        targetValue = if (timerDuration > 0) timeLeftMillis.toFloat() / timerDuration else 0f,
//        label = "timerProgress"
//    )
//
//    Column {
//        TopAppBar(
//            title = {},
//            colors = TopAppBarDefaults.topAppBarColors(
//            ),
//            actions = {
//                // Timer Button (Text Button)
//                TextButton(onClick = { showTimerMenu = true }) {
//                    Text("Timer")
//                }
//                DropdownMenu(
//                    expanded = showTimerMenu,
//                    onDismissRequest = { showTimerMenu = false }
//                ) {
//                    DropdownMenuItem(
//                        text = { Text("15 Minutes") },
//                        onClick = {
//                            timerDuration = 15 * 60 * 1000L
//                            startTimer(collectionViewModel, { timeLeftMillis = it }, { isTimerRunning = it }, timerDuration)
//                            showTimerMenu = false
//                        })
//                    DropdownMenuItem(
//                        text = { Text("20 Minutes") },
//                        onClick = {
//                            timerDuration = 20 * 60 * 1000L
//                            startTimer(collectionViewModel, { timeLeftMillis = it }, { isTimerRunning = it }, timerDuration)
//                            showTimerMenu = false
//                        })
//                    DropdownMenuItem(
//                        text = { Text("25 Minutes") },
//                        onClick = {
//                            timerDuration = 25 * 60 * 1000L
//                            startTimer(collectionViewModel, { timeLeftMillis = it }, { isTimerRunning = it }, timerDuration)
//                            showTimerMenu = false
//                        })
//                }
//
//                // Markup Button (Text Button)
//                TextButton(onClick = { showMarkupMenu = true }) {
//                    Text("Markup")
//                }
//                DropdownMenu(
//                    expanded = showMarkupMenu,
//                    onDismissRequest = { showMarkupMenu = false }
//                ) {
//                    DropdownMenuItem(text = { Text("Pen") }, onClick = { /* TODO */ })
//                    DropdownMenuItem(text = { Text("Highlight") }, onClick = { /* TODO */ })
//                    DropdownMenuItem(text = { Text("Eraser") }, onClick = { /* TODO */ })
//                }
//
//                // Resources Button (Text Button)
//                TextButton(onClick = { showResourcesMenu = true }) {
//                    Text("Digital Resources")
//                }
//
//                    DropdownMenu(
//                        expanded = showResourcesMenu,
//                        onDismissRequest = { showResourcesMenu = false }
//                    ) {
//                        DropdownMenuItem(text = { Text("Article") }, onClick = { /* TODO */ })
//                        DropdownMenuItem(text = { Text("Video") }, onClick = { /* TODO */ })
//                    }
//
//
//
//                // Timer Controls (Visible only when the timer is running)
//                if (isTimerRunning || timeLeftMillis < timerDuration) {
//                    TimerControls(
//                        isRunning = isTimerRunning,
//                        onPauseResume = {
//                            if (isTimerRunning) {
//                                timerJob?.cancel()
//                            } else {
//                                startTimer(collectionViewModel, { timeLeftMillis = it }, { isTimerRunning = it }, timeLeftMillis)
//                            }
//                            isTimerRunning = !isTimerRunning
//                        },
//                        onRestart = {
//                            timerJob?.cancel()
//                            timeLeftMillis = timerDuration
//                            startTimer(collectionViewModel, { timeLeftMillis = it }, { isTimerRunning = it }, timerDuration)
//                        },
//                        onCancel = {
//                            timerJob?.cancel()
//                            isTimerRunning = false
//                            timeLeftMillis = timerDuration
//                        }
//                    )
//                }
//            }
//        )
//
//        // Timer Progress Bar
//        if (isTimerRunning || timeLeftMillis < timerDuration) {
//            LinearProgressIndicator(
//                progress = progress,
//                modifier = Modifier.fillMaxWidth(),
//                color = if (isTimerRunning) Color.Green else Color.Yellow,
//            )
//        }
//    }
//}
//
//@Composable
//fun TimerControls(
//    isRunning: Boolean,
//    onPauseResume: () -> Unit,
//    onRestart: () -> Unit,
//    onCancel: () -> Unit
//) {
//    Row(modifier = Modifier.padding(end = 8.dp)) {
//        IconButton(onClick = onPauseResume) {
//            Icon(
//                imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
//                contentDescription = if (isRunning) "Pause" else "Resume"
//            )
//        }
//        IconButton(onClick = onRestart) {
//            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Restart")
//        }
//        IconButton(onClick = onCancel) {
//            Icon(imageVector = Icons.Filled.Cancel, contentDescription = "Cancel")
//        }
//    }
//}
//fun startTimer(viewModel: CollectionViewModel, updateTimeLeft: (Long) -> Unit, updateIsRunning: (Boolean) -> Unit,  duration: Long) {
//    updateIsRunning(true)
//    CoroutineScope(Dispatchers.Main).launch {
//        var timeLeft = duration
//        while (timeLeft > 0) {
//            delay(100) // Update every 100 milliseconds
//            timeLeft -= 100
//            updateTimeLeft(timeLeft)
//        }
//        updateIsRunning(false) // Ensure timer is marked as not running when finished
//        updateTimeLeft(duration)
//    }
//}
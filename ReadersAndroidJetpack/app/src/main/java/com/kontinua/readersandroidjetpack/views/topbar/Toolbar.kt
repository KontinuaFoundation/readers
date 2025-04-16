package com.kontinua.readersandroidjetpack.views.topbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.util.NavbarManager
import android.util.Log
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    timerViewModel: TimerViewModel,
    navbarManager: NavbarManager,
    currentChapterReferences: List<Reference>, // Use non-nullable from MainScreen derivation
    currentChapterVideos: List<Video>,         // Use non-nullable from MainScreen derivation
    onReferenceClick: (Reference) -> Unit,     // Callback for reference clicks
    onVideoClick: (Video) -> Unit              // Callback for video clicks

) {
    var showMarkupMenu by remember { mutableStateOf(false) }
    var showResourcesMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }

    val hasResources = currentChapterReferences.isNotEmpty() || currentChapterVideos.isNotEmpty()

    LaunchedEffect(currentChapterReferences, currentChapterVideos) {
        Log.d("ToolbarDebug", "Resources Updated - Videos: ${currentChapterVideos.size}, References: ${currentChapterReferences.size}")
    }

    TopAppBar(
        title = { Text("") },
        colors = TopAppBarDefaults.topAppBarColors(
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,

        ),
        navigationIcon = {
            IconButton(onClick = { navbarManager.toggleChapterSidebar() }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            TextButton(onClick = { showTimerMenu = true }) {
                Text("Timer")
            }
            DropdownMenu(
                expanded = showTimerMenu,
                onDismissRequest = { showTimerMenu = false }
            ) {
                DropdownMenuItem(text = { Text("15 Minutes") }, onClick = {
                    timerViewModel.setDurationAndReset(15 * 1000L)
                    showTimerMenu = false
                })
                DropdownMenuItem(text = { Text("20 Minutes") }, onClick = {
                    timerViewModel.setDurationAndReset(20 * 60 * 1000L)
                    showTimerMenu = false
                })
                DropdownMenuItem(text = { Text("25 Minutes") }, onClick = {
                    timerViewModel.setDurationAndReset(25 * 60 * 1000L)
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
            TextButton(
                onClick = {
                    // Add log to see if click is registered and lists before showing menu
                    Log.d("ToolbarDebug", "Resources Button Clicked. HasResources: $hasResources, Videos: ${currentChapterVideos.size}, Refs: ${currentChapterReferences.size}")
                    showResourcesMenu = true
                },
                enabled = hasResources // Disable if no resources for this chapter
            ) {
                Text("Digital Resources")
            }
            DropdownMenu(
                expanded = showResourcesMenu,
                onDismissRequest = { showResourcesMenu = false }
            ) {
                if (currentChapterVideos.isEmpty() && currentChapterReferences.isEmpty()) {
                    // This block now executes if *both* lists passed in are empty
                    DropdownMenuItem(
                        text = { Text("No resources for this chapter") },
                        onClick = { showResourcesMenu = false }, // Still allow closing
                        enabled = false // Non-interactive message
                    )
                } else {
                    // Display Videos first (if any)
                    currentChapterVideos.forEach { video ->
                        DropdownMenuItem(
                            text = { Text(video.title) },
                            onClick = {
                                onVideoClick(video)
                                showResourcesMenu = false
                            }
                        )
                    }
                    // Display References (if any)
                    currentChapterReferences.forEach { reference ->
                        DropdownMenuItem(
                            text = { Text(reference.title) },
                            onClick = {
                                onReferenceClick(reference)
                                showResourcesMenu = false
                            }
                        )
                    }
                }
            }
        }
    )

}

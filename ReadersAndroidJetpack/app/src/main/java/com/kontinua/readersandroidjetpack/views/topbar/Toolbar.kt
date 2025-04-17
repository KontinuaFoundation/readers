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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    timerViewModel: TimerViewModel,
    navbarManager: NavbarManager,
    currentChapterReferences: List<Reference>,
    currentChapterVideos: List<Video>,
    onReferenceClick: (Reference) -> Unit,
    onVideoClick: (Video) -> Unit

) {
    var showMarkupMenu by remember { mutableStateOf(false) }
    var showResourcesMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }

    val hasResources = currentChapterReferences.isNotEmpty() || currentChapterVideos.isNotEmpty()

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
                    showResourcesMenu = true
                },
                enabled = hasResources
            ) {
                Text("Digital Resources")
            }
            DropdownMenu(
                expanded = showResourcesMenu,
                onDismissRequest = { showResourcesMenu = false }
            ) {
                if (currentChapterVideos.isEmpty() && currentChapterReferences.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No resources for this chapter") },
                        onClick = { showResourcesMenu = false },
                        enabled = false
                    )
                } else {
                    //videos then texts
                    currentChapterVideos.forEach { video ->
                        DropdownMenuItem(
                            text = { Text(video.title) },
                            onClick = {
                                onVideoClick(video)
                                showResourcesMenu = false
                            }
                        )
                    }
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

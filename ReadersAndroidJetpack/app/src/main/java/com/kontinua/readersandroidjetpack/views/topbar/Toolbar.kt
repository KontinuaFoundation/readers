package com.kontinua.readersandroidjetpack.views.topbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    timerViewModel: TimerViewModel,
    navbarManager: NavbarManager,
    annotationManager: AnnotationManager,
    currentChapterReferences: Any,
    currentChapterVideos: Any,
    onReferenceClick: (Reference) -> Unit,
    onVideoClick: (Video) -> Unit
) {
    var showMarkupMenu by remember { mutableStateOf(false) }
    var showPenColorMenu by remember { mutableStateOf(false) }
    var showHighlightColorMenu by remember { mutableStateOf(false) }
    var showResourcesMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }

    val references = currentChapterReferences as? List<Reference> ?: emptyList()
    val videos = currentChapterVideos as? List<Video> ?: emptyList()

    // are there any resources? enables or disables the button
    val hasResources = references.isNotEmpty() || videos.isNotEmpty()

    TopAppBar(
        title = {
            // page navigation stuff in the title area
            PageSelector(navbarManager)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                    // currently set to 15 seconds for testing
                    // TODO: must be fixed to 15 minutes before deployment
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
            TextButton(onClick = { showMarkupMenu = true }) { Text("Markup") }
            DropdownMenu(
                expanded = showMarkupMenu,
                onDismissRequest = { showMarkupMenu = false }
            ) {
                // Pen dropdown with embedded color options
                DropdownMenuItem(
                    text = { Text("Pen") },
                    onClick = {
                        showPenColorMenu = !showPenColorMenu
                    },
                    trailingIcon = { Text("▼") }
                )

                if (showPenColorMenu) {
                    val penColors = listOf(
                        "Black" to Color.Black,
                        "Red" to Color.Red,
                        "Green" to Color.Green,
                        "Blue" to Color.Blue
                    )
                    penColors.forEach { (name, color) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            },
                            onClick = {
                                annotationManager.setPenColor(color)
                                annotationManager.togglePen(true)
                                showPenColorMenu = false
                                showMarkupMenu = false
                            }
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text("Highlighter") },
                    onClick = {
                        showHighlightColorMenu = !showHighlightColorMenu
                    },
                    trailingIcon = { Text("▼") }
                )

                if (showHighlightColorMenu) {
                    val highlightColors = listOf(
                        "Yellow" to Color.Yellow,
                        "Pink" to Color.Magenta,
                        "Green" to Color.Green,
                        "Blue" to Color.Blue
                    )
                    highlightColors.forEach { (name, color) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            },
                            onClick = {
                                annotationManager.setHighlightColor(color)
                                annotationManager.toggleHighlight(true)
                                showHighlightColorMenu = false
                                showMarkupMenu = false
                            }
                        )
                    }
                }
                DropdownMenuItem(text = { Text("Eraser") }, onClick = {
                    annotationManager.toggleScribble(true)
                    annotationManager.toggleErase(true)
                    showMarkupMenu = false
                })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Exit") }, onClick = {
                    annotationManager.toggleScribble(false)
                    showMarkupMenu = false
                })
            }

            // Resources Button (Text Button)
            TextButton(
                onClick = {
                    showResourcesMenu = true
                },
                // only on if there are resources
                enabled = hasResources
            ) {
                Text("Digital Resources")
            }
            DropdownMenu(
                expanded = showResourcesMenu,
                onDismissRequest = { showResourcesMenu = false }
            ) {
                // if there are no resources. should not dropdown, but if it does for some reason then it will just say no resources
                if (videos.isEmpty() && references.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No resources for this chapter") },
                        onClick = { showResourcesMenu = false },
                        enabled = false
                    )
                } else {
                    videos.forEach { video ->
                        DropdownMenuItem(
                            text = { Text(video.title) },
                            onClick = {
                                onVideoClick(video)
                                showResourcesMenu = false
                            }
                        )
                    }
                    references.forEach { reference ->
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

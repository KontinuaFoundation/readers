package com.kontinua.readersandroidjetpack.views.topbar

import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.ColumnScopeInstance.weight
import androidx.compose.foundation.layout.Row
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
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.util.NavbarManager

//molly changes
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

@Composable
fun Toolbar(
    timerViewModel: TimerViewModel,
    navbarManager: NavbarManager
) {
    var showMarkupMenu by remember { mutableStateOf(false) }
    var showResourcesMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }

    //pages
    var pageInputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Update the input field text when the actual current page changes externally
    LaunchedEffect(navbarManager.currentPage, navbarManager.totalPages) {
        pageInputText = if (navbarManager.totalPages > 0) {
            (navbarManager.currentPage + 1).toString() // Display 1-based page number
        } else {
            "" // Clear if no pages
        }
    }


    TopAppBar(
        title = {
            // Add Page Navigation Controls in the Title Area (or Actions)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center, // Center the page controls
//                modifier = Modifier.weight(1f) // Allow centering by taking available space
            ) {
                // Previous Page Button
                IconButton(
                    onClick = { navbarManager.goToPreviousPage() },
                    // Disable if on the first page or PDF not loaded
//                    enabled = navbarManager.currentPage > 0 && navbarManager.totalPages > 0
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Page")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Page Number Display / Input
                if (navbarManager.totalPages > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // BasicTextField for page number input
                        BasicTextField(
                            value = pageInputText,
                            onValueChange = { newValue ->
                                // Allow only digits and limit length reasonably
                                if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                    pageInputText = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done // Show "Done" action button
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val pageNum = pageInputText.toIntOrNull()
                                    if (pageNum != null) {
                                        // Convert 1-based input to 0-based index
                                        navbarManager.setPage(pageNum - 1)
                                    }
                                    // Clear focus and hide keyboard
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    // The LaunchedEffect will update pageInputText if goToPage succeeds
                                }
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy( // Use default text style
                                color = MaterialTheme.colorScheme.onSurface, // Adjust color as needed
                                textAlign = TextAlign.End, // Align input text to the right
                                fontSize = 16.sp // Match surrounding text
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), // Set cursor color
                            modifier = Modifier.width(40.dp) // Adjust width as needed
                        )

                        // Total Pages Display
                        Text(
                            text = " / ${navbarManager.totalPages}",
                            style = MaterialTheme.typography.bodyLarge, // Use appropriate style
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                else {
                    // Placeholder when PDF is not loaded
                    Text("...")
                }


                Spacer(modifier = Modifier.width(8.dp))

                // Next Page Button
                IconButton(
                    onClick = { navbarManager.goToNextPage() },
                    // Disable if on the last page or PDF not loaded
//                    enabled = navbarManager.currentPage < navbarManager.totalPages - 1 && navbarManager.totalPages > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next Page")
                }
            }
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
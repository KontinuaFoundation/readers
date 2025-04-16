package com.kontinua.readersandroidjetpack.views.topbar
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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

import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.util.NavbarManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    timerViewModel: TimerViewModel,
    navbarManager: NavbarManager
) {
    var showMarkupMenu by remember { mutableStateOf(false) }
    var showResourcesMenu by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            // Add Page Navigation Controls in the Title Area (or Actions)
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
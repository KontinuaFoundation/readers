package com.kontinua.readersandroidjetpack.views.topbar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.R
import com.kontinua.readersandroidjetpack.util.NavbarManager

//TODO: when you put in a page number that is not valid, it just jumps to the page its on, or to page 0 if its at the start which does not even exist. this should be handled cleaner.

@Composable
fun PageSelector(navbarManager: NavbarManager) {
    var pageInputText by remember { mutableStateOf(navbarManager.getAdjustedPage()) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(navbarManager.pageNumber) {
        pageInputText = navbarManager.getAdjustedPage()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // Previous Page Button
        IconButton(
            onClick = { navbarManager.goToPreviousPage() },
            enabled = navbarManager.pageNumber > 0
        ) {
            // Use standard chevron icons instead
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.prev_page)
            )
        }

        BasicTextField(
            value = pageInputText,
            onValueChange = { newValue ->
                // Only allow numeric input
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    pageInputText = newValue
                }
            },
            modifier = Modifier
                .width(48.dp)
                .border(
                    width = 1.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        // Select all text when focused
                        pageInputText = "" // This triggers recomposition
                    } else {
                        // When focus is lost, try to resolve the page number
                        resolvePageNumber(pageInputText, navbarManager) { validPage ->
                            pageInputText = validPage.toString()
                        }
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    resolvePageNumber(pageInputText, navbarManager) { validPage ->
                        pageInputText = validPage.toString()
                    }
                    // Clear focus to hide keyboard
                    focusManager.clearFocus()
                }
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )

        Text(
            text = "/ ${navbarManager.pageCount}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )

        IconButton(
            onClick = { navbarManager.goToNextPage() },
            enabled = navbarManager.pageNumber < navbarManager.pageCount - 1
        ) {
            // Use standard chevron icons instead
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.next_page)
            )
        }
    }


}

private fun resolvePageNumber(
    input: String,
    navbarManager: NavbarManager,
    onResult: (Int) -> Unit
) {
    val currentPage = navbarManager.pageNumber
    val pageCount = navbarManager.pageCount

    try {
        val newPage = input.toInt() - 1

        // Check if the new page is within the valid range (0 to pageCount-1)
        if (newPage in 0 until pageCount) {
            navbarManager.setPage(newPage)
            //onResult(newPage)
        } else {
            // Out of range, revert to the current page
            onResult(currentPage)
        }
    } catch (e: NumberFormatException) {
        // Invalid number format, revert to the current page
        onResult(currentPage)
    }
}
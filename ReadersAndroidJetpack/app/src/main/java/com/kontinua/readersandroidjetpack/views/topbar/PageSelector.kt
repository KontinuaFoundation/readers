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
//TODO: keyboard does not fully pop up when entering a page number, and i think it should.

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
                // Only allow numeric input, max 4 digits to prevent overflow
                if ((newValue.isEmpty() || newValue.all { it.isDigit() }) && newValue.length <= 4) {
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
    onResult: (String) -> Unit
) {

    val currentPage = navbarManager.pageNumber
    val pageCount = navbarManager.pageCount

    // Helper function to get the current page display string (1-based)
    fun getCurrentPageDisplayString(): String = (currentPage + 1).toString()
    if (pageCount <= 0 || input.isBlank()) {
        onResult(getCurrentPageDisplayString())
        return
    }

    val lastPage = pageCount - 1

    try {
        val desiredPageDisplayInt = input.toInt()
        val newPage = input.toInt() - 1

        when {
            // Case 1: Valid page number entered (0-based index is within range)
            newPage in 0..lastPage -> {
                navbarManager.setPage(newPage)
                onResult(desiredPageDisplayInt.toString())
            }
            // Case 2: Page number entered is too high (0-based index is >= pageCount)
            newPage >= pageCount -> {
                navbarManager.setPage(lastPage)
                onResult((lastPage + 1).toString())
            }
            // Case 3: Page number entered is too low (e.g., 0 or negative, resulting in newPage < 0)
            //this should not happen
            else -> {
                onResult(getCurrentPageDisplayString())
            }
        }
    } catch (e: NumberFormatException) {
        onResult(getCurrentPageDisplayString())
    }
}
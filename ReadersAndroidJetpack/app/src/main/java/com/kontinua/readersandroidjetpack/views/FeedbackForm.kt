package com.kontinua.readersandroidjetpack.views

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel

/**
 * Dialog component for gathering user feedback.
 * Shows only when the ViewModel's showFeedbackDialog state is true.
 * Delegates all user actions to the ViewModel.
 */
@Composable
fun FeedbackForm(
    viewModel: FeedbackViewModel,
    modifier: Modifier = Modifier
) {
    val showDialog by viewModel.feedbackVisibility.collectAsState()
    val feedbackText by viewModel.feedbackText.collectAsState()
    val context = LocalContext.current

    // Only show the dialog when the state indicates it should be shown
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = { viewModel.dismissFeedbackForm() },
        title = { Text("Provide Feedback") },
        text = {
            Column {
                Text("We value your feedback. Please let us know your thoughts or report any issues:")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { viewModel.updateFeedbackText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your feedback here") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.submitFeedback()
                    Toast.makeText(context, "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
                },
                enabled = feedbackText.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissFeedbackForm() }) {
                Text("Cancel")
            }
        }
    )
}
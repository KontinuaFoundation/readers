package com.kontinua.readersandroidjetpack.views.bottombar.feedback

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
    val context = LocalContext.current
    val showToast by viewModel.showSuccessToast.collectAsState(initial = false)

    // Put the LaunchedEffect before the early return
    LaunchedEffect(showToast) {
        if (showToast) {
            Toast.makeText(context, "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
            viewModel.resetToastTrigger()
        }
    }
    // Safely collect state values
    val showDialog by viewModel.feedbackVisibility.collectAsState()

    // Early return if dialog shouldn't be shown
    if (!showDialog) return

    // Collect state values for feedback text and email
    val feedbackText by viewModel.feedbackText.collectAsState(initial = "")
    val userEmail by viewModel.userEmail.collectAsState(initial = "")
    val submissionError by viewModel.submissionError.collectAsState(initial = null)
    val isSubmitting by viewModel.isSubmitting.collectAsState(initial = false)

    // Check if both fields have content - using null-safe and safe string operations
    val isEnabled = !isSubmitting &&
        feedbackText.trim().isNotEmpty() &&
        userEmail.trim().isNotEmpty()

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!isSubmitting) viewModel.dismissFeedbackForm()
        },
        title = { Text("Provide Feedback") },
        text = {
            Column {
                Text("We value your feedback. Please let us know your thoughts or report any issues:")
                Spacer(modifier = Modifier.height(16.dp))

                // Email field
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { viewModel.updateUserEmail(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your email here") },
                    singleLine = true,
                    label = { Text("Email") },
                    enabled = !isSubmitting
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Feedback text field
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { viewModel.updateFeedbackText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your feedback here") },
                    minLines = 3,
                    label = { Text("Feedback") },
                    enabled = !isSubmitting
                )

                // Show error if there is one
                submissionError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = androidx.compose.ui.graphics.Color.Red,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                // Show loading indicator when submitting
                if (isSubmitting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.submitFeedback()
                },
                enabled = isEnabled
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isSubmitting) viewModel.dismissFeedbackForm()
                },
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}

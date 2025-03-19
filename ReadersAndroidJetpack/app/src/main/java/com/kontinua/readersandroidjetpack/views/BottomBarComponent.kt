package com.kontinua.readersandroidjetpack.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel
import com.kontinua.readersandroidjetpack.ui.theme.buttons.FeedbackButton


/**
 * Extensible bottom app bar component that can house various action buttons.
 * Currently includes a feedback button, but can be extended with additional functionality.
 *
 * To add new functionality:
 * 1. Uncomment placeholder buttons or add new ones
 * 2. Implement click handlers
 * 3. Create corresponding dialogs/screens if needed
 */
@Composable
fun BottomBarComponent(
    feedbackViewModel: FeedbackViewModel,
    modifier: Modifier = Modifier
) {
    BottomAppBar(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
           // Left-aligned items
            // Uncomment to add Timer

            // Middle space for potential future items
            Spacer(modifier = Modifier.weight(1f))

            // Right-aligned actions
            FeedbackButton(
                onClick = { feedbackViewModel.showFeedbackForm() },
                modifier = Modifier.padding(end = 16.dp)
            )

        }
    }
}
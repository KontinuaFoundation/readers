package com.kontinua.readersandroidjetpack.ui.theme.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kontinua.readersandroidjetpack.R

/**
 * A standalone feedback button using the chat bubble drawable from resources.
 */
@Composable
fun FeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(56.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.primary,
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_chat_bubble_24),
                contentDescription = "Provide Feedback",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
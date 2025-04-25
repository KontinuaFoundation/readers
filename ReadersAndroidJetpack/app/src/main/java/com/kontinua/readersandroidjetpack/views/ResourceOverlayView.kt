package com.kontinua.readersandroidjetpack.views

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceOverlayView(
    content: Any?, // Can be Reference or Video
    onDismissRequest: () -> Unit
) {
    // Determine URL and Title based on content type
    val url: String? = when (content) {
//        is Reference -> content.url
        is Video -> content.url // Assuming Video also has a 'url' property
        else -> null
    }
    val title: String = when (content) {
        is Reference -> content.title
        is Video -> content.title
        else -> "Resource" // Fallback title
    }

    if (url == null) {
        // Handle cases where content is null or doesn't have a URL unexpectedly
        // You might want to log an error or show a message
        // For now, we just don't show anything if URL is null
        return
    }

    // Semi-transparent background overlay to catch dismiss clicks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                // Make the background clickable for dismissal, without ripple effect
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center // Center the card
    ) {
        // Prevent clicks on the card from propagating to the background dismiss
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Take 90% of width
                .fillMaxHeight(0.85f) // Take 85% of height
                .clickable( // Add this empty clickable to consume clicks
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}, // Empty lambda consumes the click
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                // Top bar within the card for back button and title
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1, // Prevent title wrap
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Close Resource")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant // Or other appropriate color
                    )
                )

                // WebView to display the content
                AndroidView(
                    modifier = Modifier.fillMaxSize(), // Fill remaining space in the Card
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = WebViewClient() // Basic WebViewClient
//                            settings.javaScriptEnabled = true // Enable JS if needed
                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        // Can update settings or reload URL if needed,
                        // but typically factory is sufficient for initial load.
                        // Check if URL changed (though unlikely with this state management)
                        if (webView.originalUrl != url) {
                            webView.loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}

// Helper function to get the URL - adjust if Video structure differs
private val Video.url: String?
    get() = this.url // Assuming Video has a public 'url' property. Change if needed.

// Reference already has a url property, so no extension needed.
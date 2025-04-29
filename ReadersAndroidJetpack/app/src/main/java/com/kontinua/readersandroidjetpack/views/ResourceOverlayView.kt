package com.kontinua.readersandroidjetpack.views

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceOverlayView(
    content: Any?, // Can be Reference or Video
    onDismissRequest: () -> Unit
) {
    val url: String? = when (content) {
        is Reference -> content.link
        is Video -> content.link
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                // Make the background clickable for dismissal, without ripple effect
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
        // Prevent clicks on the card from propagating to the background dismiss
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Resource")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // WebView to display the content
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = WebViewClient()
                            //needs to be enabled for the videos to be able to play
                            settings.javaScriptEnabled = true
                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        if (webView.originalUrl != url) {
                            webView.loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}
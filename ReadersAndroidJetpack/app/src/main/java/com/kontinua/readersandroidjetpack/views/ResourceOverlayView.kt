package com.kontinua.readersandroidjetpack.views

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video
import java.util.regex.Pattern

// Helper function to extract YouTube video ID from diff URL formats
// tbh got this function from chat so it could be bad but it's working at least
// Helper function to extract YouTube video ID from various URL formats
fun extractYouTubeVideoId(youtubeUrl: String?): String? {
    if (youtubeUrl.isNullOrBlank()) {
        return null
    }
    // Pattern to cover various YouTube URL formats (watch, youtu.be, embed, shorts, live)
    // stupid strategy to combat the linter saying the line was too long
    val patternString =
        "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|" +
            "watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|" +
            "%2Fv%2F|e\\/|shorts\\/|live\\/)[^#&?\\n]*"
    val compiledPattern = Pattern.compile(patternString)
    val matcher = compiledPattern.matcher(youtubeUrl)
    return if (matcher.find()) {
        matcher.group()
    } else {
        null
    }
}

// Helper function to generate embed HTML for a YouTube video
// tbh got this function from chat too so it could also be bad but it's working at least
fun getYouTubeEmbedHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body, html { margin: 0; padding: 0; height: 100%; overflow: hidden; background-color: #000; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <iframe 
                src="https://www.youtube.com/embed/$videoId?autoplay=0&controls=1&modestbranding=1&rel=0&showinfo=0"
                frameborder="0" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()
}

// Define a sealed class to represent the content loading strategy for the WebView
// basically says do normal unless its a youtube video and then do the new thing
sealed class WebViewLoadStrategy {
    data class LoadUrl(val url: String) : WebViewLoadStrategy()
    data class LoadHtml(
        val htmlContent: String,
        val baseUrl: String = "https://www.youtube.com" // base url
    ) : WebViewLoadStrategy()
    data object None : WebViewLoadStrategy()
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionName")
fun ResourceOverlayView(
    content: Any?, // Can be Reference or Video
    onDismissRequest: () -> Unit
) {
    val originalUrl: String? =
        when (content) {
            is Reference -> content.link
            is Video -> content.link
            else -> null
        }
    val title: String =
        when (content) {
            is Reference -> content.title
            is Video -> content.title
            else -> "Resource" // Fallback title
        }

    // Determine the loading strategy based on the content type and URL
    val loadStrategy: WebViewLoadStrategy = remember(originalUrl, content) {
        // this should not happen but just in case
        if (originalUrl == null) {
            WebViewLoadStrategy.None
        } else if (content is Video) { // Only attempt embed for Video type
            val videoId = extractYouTubeVideoId(originalUrl)
            if (videoId != null) {
                // if its a video, try and do the youtube thing
                WebViewLoadStrategy.LoadHtml(getYouTubeEmbedHtml(videoId))
            } else {
                // if not youtube, do regular load
                WebViewLoadStrategy.LoadUrl(originalUrl)
            }
        } else {
            // load regular if not video
            WebViewLoadStrategy.LoadUrl(originalUrl)
        }
    }
    var webViewInstance: WebView? by remember { mutableStateOf(null) }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}, // Prevent clicks on the card from propagating
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Resource")
                        }
                    },
                    colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewInstance = this // Store the instance to display
                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            webViewClient = object : WebViewClient() {
                            }
                            webChromeClient = WebChromeClient() // fullscreen stuff
                            settings.javaScriptEnabled = true

                            // do the load as we picked
                            when (loadStrategy) {
                                is WebViewLoadStrategy.LoadUrl -> loadUrl(loadStrategy.url)
                                is WebViewLoadStrategy.LoadHtml -> loadDataWithBaseURL(
                                    loadStrategy.baseUrl,
                                    loadStrategy.htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                WebViewLoadStrategy.None -> {}
                            }
                        }
                    },
                    update = { webView ->
                        // calls if the load strat changes
                        when (loadStrategy) {
                            is WebViewLoadStrategy.LoadUrl -> {
                                if (webView.url != loadStrategy.url) {
                                    webView.loadUrl(loadStrategy.url)
                                }
                            }
                            is WebViewLoadStrategy.LoadHtml -> {
                                webView.loadDataWithBaseURL(
                                    loadStrategy.baseUrl,
                                    loadStrategy.htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                            WebViewLoadStrategy.None -> {}
                        }
                    }
                )
            }
        }
    }
}

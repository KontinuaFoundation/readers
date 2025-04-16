// MainActivity.kt
package com.kontinua.readersandroidjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.views.SidebarWithPDFViewer
import com.kontinua.readersandroidjetpack.views.bottombar.BottomBarComponent
import com.kontinua.readersandroidjetpack.views.bottombar.feedback.FeedbackForm
import com.kontinua.readersandroidjetpack.views.bottombar.timer.TimerProgressIndicator
import com.kontinua.readersandroidjetpack.views.topbar.Toolbar

//molly changes
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadersAndroidJetpackTheme {
                MainScreen() // Use a composable to organize the UI
            }
        }
    }
}

@Composable
fun MainScreen() {
    val timerViewModel: TimerViewModel = viewModel()
    val feedbackViewModel: FeedbackViewModel = viewModel()
    val navbarManager = remember { NavbarManager() }
    val collectionViewModel: CollectionViewModel = viewModel()

    LaunchedEffect(collectionViewModel) {
        navbarManager.setCollection(collectionViewModel)
    }
//    val currentPageNumber = navbarManager.pageNumber
    val chapters by collectionViewModel.chaptersState.collectAsState()
    val currentChapterIndexState = navbarManager.currentChapterIndex
    val currentChapterResources = remember(currentChapterIndexState, chapters) {
        val currentChapter = navbarManager.getCurrentChapter()

        if (currentChapter != null) {
            val videos = currentChapter.covers.flatMap { it.videos ?: emptyList() }
            val references = currentChapter.covers.flatMap { it.references ?: emptyList() }
            Pair(videos, references) // Return pair of lists
        } else {
            Pair(emptyList<Video>(), emptyList<Reference>()) // Return empty lists if no chapter
        }
    }
    val currentChapterVideos = currentChapterResources.first
    val currentChapterReferences = currentChapterResources.second

    val context = LocalContext.current
    val onReferenceClick = remember<(Reference) -> Unit> { // Remember the lambda
        { reference ->
            val intent = Intent(Intent.ACTION_VIEW, reference.link.toUri())
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Cannot open link: ${reference.link}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val onVideoClick = remember<(Video) -> Unit> { // Remember the lambda
        { video ->
            val intent = Intent(Intent.ACTION_VIEW, video.link.toUri())
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Cannot open video: ${video.link}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Toolbar(
                timerViewModel = timerViewModel,
                navbarManager = navbarManager,
                currentChapterReferences = currentChapterReferences,
                currentChapterVideos = currentChapterVideos,
                onReferenceClick = onReferenceClick,
                onVideoClick = onVideoClick
            )
        },
        bottomBar = {
            Column {
                // Timer progress indicator above the bottom bar
                TimerProgressIndicator(timerViewModel = timerViewModel)

                // Unified bottom bar with timer controls and feedback button
                BottomBarComponent(
                    feedbackViewModel = feedbackViewModel,
                    timerViewModel = timerViewModel
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SidebarWithPDFViewer(navbarManager = navbarManager, collectionViewModel = collectionViewModel)
        }
        FeedbackForm(viewModel = feedbackViewModel)
    }
}
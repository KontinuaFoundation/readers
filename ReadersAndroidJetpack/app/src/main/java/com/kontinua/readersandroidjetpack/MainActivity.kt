// MainActivity.kt
package com.kontinua.readersandroidjetpack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.util.ChapterContentManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModelFactory
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.views.SidebarWithPDFViewer
import com.kontinua.readersandroidjetpack.views.bottombar.BottomBarComponent
import com.kontinua.readersandroidjetpack.views.bottombar.feedback.FeedbackForm
import com.kontinua.readersandroidjetpack.views.bottombar.timer.TimerProgressIndicator
import com.kontinua.readersandroidjetpack.views.topbar.Toolbar

// TODO: add a loading screen of some sort while the PDF is getting fetched

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

    @Composable
    fun MainScreen() {
        val timerViewModel: TimerViewModel = viewModel()
        val navbarManager = remember { NavbarManager() }
        val feedbackViewModel: FeedbackViewModel = viewModel(
            factory = FeedbackViewModelFactory(navbarManager)
        )

        val collectionViewModel: CollectionViewModel = viewModel()
        LaunchedEffect(collectionViewModel) {
            navbarManager.setCollection(collectionViewModel)
        }
        val chapterContentManager = remember(navbarManager) {
            ChapterContentManager(
                navbarManager = navbarManager
            )
        }

        val currentChapterReferences by remember(navbarManager.currentChapterIndex) {
            derivedStateOf { chapterContentManager.getReferencesForCurrentChapter() }
        }

        val currentChapterVideos by remember(navbarManager.currentChapterIndex) {
            derivedStateOf { chapterContentManager.getVideosForCurrentChapter() }
        }

        val selectedReference = remember { mutableStateOf<Reference?>(null) }
        val selectedVideo = remember { mutableStateOf<Video?>(null) }

        val handleReferenceClick: (Reference) -> Unit = { reference ->
            selectedReference.value = reference
            // TODO: Add logic here if you need to DO something when a reference is clicked
            // e.g., open a browser, show details (customview in the future)
            Log.d("MainActivity", "Reference clicked: ${reference.title}")
        }

        val handleVideoClick: (Video) -> Unit = { video ->
            selectedVideo.value = video
            // TODO: Add logic here if you need to DO something when a video is clicked
            // e.g., open a video player (customview in the future)
            Log.d("MainActivity", "Video clicked: ${video.title}")
        }

        Scaffold(
            topBar = {
                Toolbar(
                    timerViewModel = timerViewModel,
                    navbarManager = navbarManager,
                    // now need to pass this stuff to toolbar
                    currentChapterReferences = currentChapterReferences,
                    currentChapterVideos = currentChapterVideos,
                    onReferenceClick = handleReferenceClick,
                    onVideoClick = handleVideoClick
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
                // need to pass collectionview down to sidebar with pdf
                SidebarWithPDFViewer(navbarManager = navbarManager, collectionViewModel = collectionViewModel)
            }
            FeedbackForm(viewModel = feedbackViewModel)
        }
    }
}

// MainActivity.kt
package com.kontinua.readersandroidjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.util.AnnotationManager
import com.kontinua.readersandroidjetpack.util.ChapterContentManager
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModelFactory
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.views.PDFViewer
import com.kontinua.readersandroidjetpack.views.ResourceOverlayView
import com.kontinua.readersandroidjetpack.views.UnifiedSidebar
import com.kontinua.readersandroidjetpack.views.bottombar.BottomBarComponent
import com.kontinua.readersandroidjetpack.views.bottombar.timer.TimerProgressIndicator
import com.kontinua.readersandroidjetpack.views.topbar.Toolbar
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import androidx.compose.ui.platform.LocalContext


// TODO: add a loading screen of some sort while the PDF is getting fetched

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()

        setContent {
            ReadersAndroidJetpackTheme {
                MainScreen() // Use a composable to organize the UI
            }
        }
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        val timerViewModel: TimerViewModel = viewModel()
        val navbarManager = remember { NavbarManager(context) }
        val annotationManager = remember { AnnotationManager() }
        val feedbackViewModel: FeedbackViewModel = viewModel(factory = FeedbackViewModelFactory(navbarManager))
        val collectionViewModel: CollectionViewModel = viewModel()
        val chapterContentManager =
            remember(navbarManager) {
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

        var overlayContent by remember { mutableStateOf<Any?>(null) }

        val handleReferenceClick: (Reference) -> Unit = { reference ->
            overlayContent = reference
        }

        val handleVideoClick: (Video) -> Unit = { video ->
            overlayContent = video
        }

        LaunchedEffect(collectionViewModel) {
            navbarManager.setCollection(collectionViewModel)
        }

        val dismissOverlay: () -> Unit = { overlayContent = null }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    Toolbar(
                        timerViewModel = timerViewModel,
                        navbarManager = navbarManager,
                        currentChapterReferences = currentChapterReferences,
                        currentChapterVideos = currentChapterVideos,
                        onReferenceClick = handleReferenceClick,
                        onVideoClick = handleVideoClick,
                        annotationManager = annotationManager
                    )
                },
                bottomBar = {
                    Column {
                        TimerProgressIndicator(timerViewModel)
                        BottomBarComponent(feedbackViewModel, timerViewModel)
                    }
                },
                content = { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        PDFViewer(
                            navbarManager = navbarManager,
                            collectionViewModel = collectionViewModel,
                            annotationManager = annotationManager
                        )
                    }
                }
            )
        }

        UnifiedSidebar(
            navbarManager = navbarManager
        )

        if (overlayContent != null) {
            ResourceOverlayView(
                content = overlayContent,
                onDismissRequest = dismissOverlay
            )
        }
    }
}

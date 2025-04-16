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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModelFactory
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.views.SidebarWithPDFViewer
import com.kontinua.readersandroidjetpack.views.bottombar.BottomBarComponent
import com.kontinua.readersandroidjetpack.views.bottombar.feedback.FeedbackForm
import com.kontinua.readersandroidjetpack.views.bottombar.timer.TimerProgressIndicator
import com.kontinua.readersandroidjetpack.views.topbar.Toolbar

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
    val navbarManager = remember { NavbarManager() }
    val feedbackViewModel: FeedbackViewModel = viewModel(
        factory = FeedbackViewModelFactory(navbarManager)
    )

    Scaffold(
        topBar = {
            Toolbar(
                timerViewModel = timerViewModel,
                navbarManager = navbarManager
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
            SidebarWithPDFViewer(navbarManager = navbarManager)
        }
        FeedbackForm(viewModel = feedbackViewModel)
    }
}
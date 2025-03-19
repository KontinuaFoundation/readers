package com.kontinua.readersandroidjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontinua.readersandroidjetpack.views.BottomBarComponent
import com.kontinua.readersandroidjetpack.viewmodels.FeedbackViewModel
import com.kontinua.readersandroidjetpack.views.FeedbackForm
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.util.NavbarManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ReadersAndroidJetpackTheme {
                // Initialize the FeedbackViewModel
                val feedbackViewModel: FeedbackViewModel = viewModel()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomBarComponent(feedbackViewModel = feedbackViewModel) }
                ) { innerPadding ->
                    // Pass the feedbackViewModel to FeedbackForm
                    FeedbackForm(viewModel = feedbackViewModel)

                    // Use SidebarWithPDFViewer inside the Scaffold content
                    // with proper padding from the scaffold's innerPadding
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        com.kontinua.readersandroidjetpack.views.SidebarWithPDFViewer()
                    }
                }
            }
        }
    }
}
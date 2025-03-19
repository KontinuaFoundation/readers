package com.kontinua.readersandroidjetpack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.views.Toolbar
import com.kontinua.readersandroidjetpack.views.PDFViewer
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import com.kontinua.readersandroidjetpack.views.TimerBottomBar
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel // Import TimerViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ReadersAndroidJetpackTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val collectionViewModel: CollectionViewModel = viewModel()
    val timerViewModel: TimerViewModel = viewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Toolbar(timerViewModel = timerViewModel)
        },
        bottomBar = {
            TimerBottomBar(timerViewModel = timerViewModel)
        }
    ) { innerPadding ->
        PDFViewer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
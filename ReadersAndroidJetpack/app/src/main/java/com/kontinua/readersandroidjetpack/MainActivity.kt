// MainActivity.kt
package com.kontinua.readersandroidjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
import com.kontinua.readersandroidjetpack.viewmodels.TimerViewModel
import com.kontinua.readersandroidjetpack.views.SidebarWithPDFViewer
import com.kontinua.readersandroidjetpack.views.TimerBottomBar
import com.kontinua.readersandroidjetpack.views.Toolbar

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

    Scaffold(
        topBar = {
            Toolbar(timerViewModel = timerViewModel)
        },
        bottomBar = {
            TimerBottomBar(timerViewModel = timerViewModel)
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SidebarWithPDFViewer()
        }
    }
}
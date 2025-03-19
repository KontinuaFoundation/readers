//package com.kontinua.readersandroidjetpack
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.Column
//
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Scaffold
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
//import com.kontinua.readersandroidjetpack.views.Toolbar
//import com.kontinua.readersandroidjetpack.views.PDFViewer
//import com.kontinua.readersandroidjetpack.viewmodels.CollectionViewModel
//import com.kontinua.readersandroidjetpack.views.TimerBottomBar
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        setContent {
//            ReadersAndroidJetpackTheme {
//                MainScreen()
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MainScreen() {
//    val collectionViewModel: CollectionViewModel = viewModel()
//
//    Scaffold(
//        modifier = Modifier.fillMaxSize(),
//        topBar = {
//            Toolbar(collectionViewModel = collectionViewModel) //Markup + Resources now only
//        },
//        bottomBar = {
//            TimerBottomBar(collectionViewModel = collectionViewModel) // New Bottom Bar
//        }
//    ) { innerPadding ->
//        Column(modifier = Modifier.padding(innerPadding)) {
//            PDFViewer(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .weight(1f), // Fill remaining space, but allow BottomAppBar to have its height
////                collectionViewModel = collectionViewModel
//            )
//
//        }
//    }
//}
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
    val collectionViewModel: CollectionViewModel = viewModel() // Get CollectionViewModel
    val timerViewModel: TimerViewModel = viewModel() // Get TimerViewModel

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Toolbar(collectionViewModel = collectionViewModel, timerViewModel = timerViewModel) // Pass both ViewModels
        },
        bottomBar = {
            TimerBottomBar(timerViewModel = timerViewModel) // Pass TimerViewModel
        }
    ) { innerPadding ->
        PDFViewer( // Assuming you have a PDFViewer composable
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
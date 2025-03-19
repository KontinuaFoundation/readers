package com.kontinua.readersandroidjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import com.kontinua.readersandroidjetpack.util.NavbarManager
import com.kontinua.readersandroidjetpack.views.SidebarWithPDFViewer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ReadersAndroidJetpackTheme {
                SidebarWithPDFViewer()
            }
        }
    }


}


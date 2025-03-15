package com.kontinua.readersandroidjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.kontinua.readersandroidjetpack.ui.theme.ReadersAndroidJetpackTheme
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            try {
                val collections = APIManager.getCollections()
                Log.d("collections", collections.toString())
            } catch (e: Exception) {
                Log.e("collections", "Error fetching collections", e)
            }
        }

        setContent {
            ReadersAndroidJetpackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PdfViewerScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        pdfUrl = "https://kontinua-foundation-workbook-pdfs.s3.us-east-2.amazonaws.com/workbook-04-en-US-1.0.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASDRANPQQ2D4O376I%2F20250315%2Fus-east-2%2Fs3%2Faws4_request&X-Amz-Date=20250315T042715Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Security-Token=IQoJb3JpZ2luX2VjELT%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMiJHMEUCIH8ogE7NP3DvqfnMqKeu5QW%2F8wVBI9Nry7v2cL5KyWtmAiEAzatv%2FDo60OFq55AbfKl1UknDsS4KJkrCETN2z750p4YqxQUI%2Ff%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAAGgwxNDUwMjMxMzg4NDkiDLsElkDO%2FBWJXLlv%2BCqZBdVrzMxk%2FNn5Pn2qEEgXf8KOrR1hbF5LnErDhztx6a2rAdE7G6%2B8t9CPNDQdbSadvPpe8KEkpbzeGFM4ahoebqQ5X8UB78RATDrWeUjKWeKR1q5vy7mkbeKFzrj%2FUPknT%2FUhAkBScVFAQaXAYWBzbFnfM3y%2B8gSrj1KoJQ1a8CyWGBT4n3Zr5E93V2UVrwEgFjNzWzM9oGzrtZ%2F5LvIRjcJF8BzwgqkGLyoqaGx8C3mxKLplTPCtYIbICdzdDEQ9LAmW%2BLzrU7Ktgqo1b1%2BC6XPhZGyzPBt%2BaLDZiVdAS55crvxojNM91Y5Pb0szc6BNirzksIGCmTCS7ikeNYszolt4ZEsMYgLJGVFPrlmcX3nDWvY8tTr65YhopivoLiQGJQDuMz2Ra%2F79BqJ2vMRnnaZuQS2HyBK5RhSVQ6zxZ%2F4m5idp6nLkNYcdnfqJcZLtXzQ1y50lljLBpF0gFW0uxqLXbdtMj%2B%2F1NeqSp831cjJkX04uJEtK%2BF8s1CRZuWrmwiNF2EO3w9uM3eH4qT6tujhggGn8ilTjlWh6%2BOGMJiGlV5ZTRuOur9jiB1R5%2BAAqkc41pXC79LyVTxj20j3VifBBILKm8pSAdynetD7Q%2FseOhMoGGoaVDWWmu1%2FKUqSVHHz8uv%2BMFo2nRTBvVh6C0RZpiQNLGkP5S1Dj4jAPXET1PV72I9Wdb0B50jQdEC41VBysBxGAyQuKFawkZcrZYxY8%2FHM6JjIX1I3LIpmwztERUnQ9db2wcknjLQYBWNzzYTGVnXyi0hw5isZA9AkSNM3o28YriBVti9q4fiZmaliITwh0%2FVm9axMMcamTby0vHqpR9Mdm4Z1ZWjfKzjf9vs%2B7YLA6GJugyfCxKQra0%2BIKTo5ellV%2BgBVhMMjy074GOrEBHcQHRc%2BKAqXG8Pi4EpKWC09ntKLE%2FW5gBU7hgvRZsavumlYC8aU6Kiu9hL4DOGG%2FFF8DS9xCXoSObg2TYJyS%2BhKUKCd9uGKyjmrav0aSHKocgefipiUK8LVrMTF7fbk2OgEWPD09cTdn66ZkN6UzRw%2Fyh7f6SzsTN1ZQw6Gi7iYaEZ1uLpSpo73wLAIW2IV0WfVLzDXuz%2BgOAijlhcilQXiG2VtqIEEKz5xvFgUTZZVS&X-Amz-Signature=47d8a710c91121dfbe4e85acbfc2b87414e43b29647a2a6e14cbbf6f06c75e9d"
                    )
                }
            }
        }
    }
}


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
                val collection = APIManager.getLatestCollection()

                val firstWorkbookPreview = collection?.workbooks?.first()

                if (firstWorkbookPreview == null) {
                    throw Exception("Couldn't get first workbook")
                }

                val firstWorkbook = APIManager.getWorkbook(firstWorkbookPreview)

                Log.d("workbook", firstWorkbook.toString())
            } catch (e: Exception) {
                Log.e("workbook", "Error fetching collections", e)
            }
        }

        setContent {
            ReadersAndroidJetpackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PdfViewerScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        pdfUrl = "https://kontinua-foundation-workbook-pdfs.s3.us-east-2.amazonaws.com/workbook-01-en-US-1.0.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASDRANPQQRRJ57MAC%2F20250316%2Fus-east-2%2Fs3%2Faws4_request&X-Amz-Date=20250316T000419Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEMj%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMiJHMEUCIEnBHF9D2VgR1XUwxC5dS%2FWfp7Bji1zL1PPZZubLzsFpAiEA%2BZJyQhKkTKAbQT1MfV%2Bc8qNGxrZSHakw%2BeoPWfIqwScqvAUIIRAAGgwxNDUwMjMxMzg4NDkiDJOoAlgy78Ie3jGReyqZBZ%2F51WbQS%2FIAiVb0%2FF4kkwgQl%2Bnt2FO%2Fyg4VqdK6NdEvuC0ADmp3ruU9NGnWo%2BctEtrK%2BsYkeNU%2FjUFcE0djq01oS1h%2FeaVv3SrxHmMNRvJQk1B61ip1H3H%2FlCl238db2CSC757HVzy%2BPJddZ8YbasK5ytc22F2uMauRh%2BUy996oRAyReptYP5JkshkG3fAR%2Fc40EyuhzNOQKq2zspmYhLFAfjNai%2Fx%2FQ0rM8svif%2Berampji0ndJU3LPv7BIPdRMG9Y84NBF3q46HXhJmiVKZCKbfadJZ3RZkPWg0LVB9DbkyPLO8qTQoZFwtOqH9yJ5s1Ek0SZGD149N6vuIAV3Baag17l4vZPUyS%2FalZPrT87N%2FXzSn%2F9D8ibXdUFLDOAJc0TWwyxnUC7X1ipIAWEeQTpStuogUwLgqH9yIsDeh0sZf2NTeqIQENCJ2ug52piBS3qSlvzf3ZgD%2FryMVqq0opi8qvKMmUwQvPJAfj9fBDwi1zdnmdFi%2FA15vAtB8%2FGc7INTQoH9aoUHmbGtjtel0ytwk6pz%2BGGLXwpqcqglWyoJMYDoGqNoZF6lwnjiJVqa79Ai1I4UUOnELz%2BUP4pS%2FxLHXj2RfgGwkBeErTSNS3Nysq0GF%2Berd0BMVX82qKKeRO2ffJcCDDOuEc5bIgAYa78QhGpCGKJ97QuXw3NjrlLdgUlgsn2BLdyKgB9OlthgCkMvabjV6MD82ePFNQOa2ZxqFQpgjQpqQuE%2F0EQJBhcJj5lfry93WGgsUbF6In549MKs3h8i4%2F338LRJ7W%2FCvUhfGn8ovtkFIgLz4wSQXKUKY3lqKCIT7fEFUtx6MG0RCHNz46QC5TwLGtGf%2B3OvnBT2vFT5omUMetjbBrLu9L%2FQ9GcB%2FnVryu6MLGf2L4GOrEB7c56MxqAATZMOWhzctT6H5geh%2FGnChZyUjCTBEufSw7N3Atha6em%2FTJP4dgi%2BQKwmBqEFeQm3lzhyKv0qVHH3OJFKmKUqNx33DZUAu%2BJtJWHXyGmpfsxHt5oJxLocWNSyQ%2F0Blskm4yDPbJ6ZUmTYrE9lcMtCMmZmexnRcUdwGKA5CwZHHBUln08DtnAseScCODMQxrtRk5Z4uyWyQiLgDy0YIJ3CK%2BBA1CY%2Fngq2A%2Fg&X-Amz-Signature=7a795bf235a4b8d9c26cca3e8ff926faf92d539a534249cee8abe30d4b78d6d2"
                    )
                }
            }
        }
    }
}


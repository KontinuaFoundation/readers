package com.kontinua.readersandroidjetpack.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SearchResult(
    val page: Int,
    val snippet: String
)

class SearchManager {
    private var pageTexts: List<String> = emptyList()
    
    /** Loads and indexes every page’s text. Call once per new PDF file. */
    suspend fun loadPdf(file: File) = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { doc ->
            val stripper = PDFTextStripper()
            pageTexts = (1..doc.numberOfPages).map { pageNum ->
                stripper.startPage = pageNum
                stripper.endPage   = pageNum
                // collapse whitespace so snippets look nicer
                stripper.getText(doc)
                    .trim()
                    .replace(Regex("\\s+"), " ")
            }
        }
    }

    /**
     * Returns all pages where the query appears,
     * with a little context snippet around each hit.
     */
    fun search(query: String): List<SearchResult> {
        val q = query.trim().lowercase()
        return pageTexts.mapIndexedNotNull { index, text ->
            val idx = text.lowercase().indexOf(q).takeIf { it >= 0 } ?: return@mapIndexedNotNull null
            // grab up to 30 chars before/after for context
            val start = (idx - 30).coerceAtLeast(0)
            val end   = (idx + q.length + 30).coerceAtMost(text.length)
            val snippet = text.substring(start, end).trim().let {
                // add ellipses only if we cut off
                (if (start > 0) "…" else "") + it + (if (end < text.length) "…" else "")
            }
            SearchResult(page = index, snippet = snippet)
        }
    }
}

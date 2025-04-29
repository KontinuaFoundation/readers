package com.kontinua.readersandroidjetpack.util

import com.kontinua.readersandroidjetpack.serialization.Chapter
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video

// tbh this whole class might be unncessary, but i do like the fact that it moves some stuff out of mainactivity
class ChapterContentManager(
    private val navbarManager: NavbarManager
) {
    // Helper function to get the current chapter based on navbar state
    private fun getCurrentChapter(): Chapter? {
        return navbarManager.getCurrentChapter()
    }

    // Function to calculate references for the current chapter
    fun getReferencesForCurrentChapter(): List<Reference> {
        val currentChapter = getCurrentChapter()
        return currentChapter?.covers?.flatMap { it.references ?: emptyList() } ?: emptyList()
    }

    // Function to calculate videos for the current chapter
    fun getVideosForCurrentChapter(): List<Video> {
        val currentChapter = getCurrentChapter()
        return currentChapter?.covers?.flatMap { it.videos ?: emptyList() } ?: emptyList()
    }
}

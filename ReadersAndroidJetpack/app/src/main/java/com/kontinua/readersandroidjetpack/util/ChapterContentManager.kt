package com.kontinua.readersandroidjetpack.util

import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video
import com.kontinua.readersandroidjetpack.serialization.Chapter

class ChapterContentManager(
    private val navbarManager: NavbarManager
) {
    // Helper function to get the current chapter based on navbar state
    private fun getCurrentChapter(): Chapter? {
        val chapters = navbarManager.collectionVM?.chapters ?: return null
        val index = navbarManager.currentChapterIndex
        return chapters.getOrNull(index)
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
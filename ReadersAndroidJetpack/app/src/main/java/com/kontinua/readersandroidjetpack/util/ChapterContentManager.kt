package com.kontinua.readersandroidjetpack.util
import com.kontinua.readersandroidjetpack.serialization.Reference
import com.kontinua.readersandroidjetpack.serialization.Video

// No longer needs allReferences/allVideos in constructor
class ChapterContentManager(
    private val navbarManager: NavbarManager
) {
    // Calculate current references on demand
    val currentReferences: List<Reference>
        get() {
            // Get the current chapters list from the CollectionViewModel via NavbarManager
            val chapters = navbarManager.collectionVM?.chapters ?: emptyList()
            // Get the specific chapter based on the current index
            val currentChapter = chapters.getOrNull(navbarManager.currentChapterIndex)
            // Extract references from that chapter, or return empty list if chapter/references are null/empty
            return currentChapter?.covers?.flatMap { it.references ?: emptyList() } ?: emptyList()
        }

    // Calculate current videos on demand
    val currentVideos: List<Video>
        get() {
            // Get the current chapters list from the CollectionViewModel via NavbarManager
            val chapters = navbarManager.collectionVM?.chapters ?: emptyList()
            // Get the specific chapter based on the current index
            val currentChapter = chapters.getOrNull(navbarManager.currentChapterIndex)
            // Extract videos from that chapter, or return empty list if chapter/videos are null/empty
            return currentChapter?.covers?.flatMap { it.videos ?: emptyList() } ?: emptyList()
        }
}


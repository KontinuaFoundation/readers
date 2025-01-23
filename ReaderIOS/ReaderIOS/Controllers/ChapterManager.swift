//
//  ChapterManager.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 1/22/25.
//

class ChapterManager {
    // Store chapters sorted by start page for efficient lookup
    private var sortedChapters: [Chapter]
    // Cache the last found chapter for optimization
    private var lastFoundChapter: Chapter?

    init(chapters: [Chapter]) {
        // Sort chapters by start page for binary search
        sortedChapters = chapters.sorted { $0.startPage < $1.startPage }
    }

    /// Returns the chapter that contains the given page number
    /// - Parameter pageNumber: The current page number (0-based)
    /// - Returns: The chapter containing this page, or nil if not found
    func getChapter(forPage pageNumber: Int) -> Chapter? {
        // Quick check if we're still in the same chapter
        if let lastChapter = lastFoundChapter,
           isPage(pageNumber, inChapter: lastChapter)
        {
            return lastChapter
        }

        // Binary search for the appropriate chapter
        let chapter = findChapter(containingPage: pageNumber)
        lastFoundChapter = chapter
        return chapter
    }

    /// Performs binary search to find the chapter containing the given page
    private func findChapter(containingPage pageNumber: Int) -> Chapter? {
        var left = 0
        var right = sortedChapters.count - 1

        while left <= right {
            let mid = (left + right) / 2
            let chapter = sortedChapters[mid]

            // If this is the last chapter, check if the page number is >= its start page
            if mid == sortedChapters.count - 1 {
                return pageNumber >= chapter.startPage - 1 ? chapter : nil
            }

            // Check if the page is in the current chapter's range
            let nextChapterStart = sortedChapters[mid + 1].startPage - 1
            if pageNumber >= chapter.startPage - 1, pageNumber < nextChapterStart {
                return chapter
            }

            // Adjust search range
            if pageNumber < chapter.startPage - 1 {
                right = mid - 1
            } else {
                left = mid + 1
            }
        }

        return nil
    }

    /// Checks if a page number falls within a chapter's range
    private func isPage(_ pageNumber: Int, inChapter chapter: Chapter) -> Bool {
        let chapterIndex = sortedChapters.firstIndex(where: { $0.id == chapter.id })!
        let nextChapterStartPage = chapterIndex < sortedChapters.count - 1 ?
            sortedChapters[chapterIndex + 1].startPage - 1 : Int.max

        return pageNumber >= chapter.startPage - 1 && pageNumber < nextChapterStartPage
    }

    /// Updates the list of chapters
    /// - Parameter chapters: New array of chapters
    func updateChapters(_ chapters: [Chapter]) {
        sortedChapters = chapters.sorted { $0.startPage < $1.startPage }
        lastFoundChapter = nil
    }
}

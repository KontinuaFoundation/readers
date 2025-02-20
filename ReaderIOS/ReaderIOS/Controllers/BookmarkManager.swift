//
//  BookmarkManager.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import Combine
import Foundation

final class BookmarkManager: ObservableObject {
    @Published var bookmarkLookup: [String: Set<Int>] = [:]

    /// Checks if the current page is bookmarked for the given file.
    func isBookmarked(fileName: String?, currentPage: Int) -> Bool {
        guard let fileName = fileName else { return false }
        return bookmarkLookup[fileName]?.contains(currentPage) ?? false
    }

    /// Toggles the bookmark status for the given file and page.
    func toggleBookmark(for fileName: String?, currentPage: Int) {
        guard let fileName = fileName else { return }
        if var pages = bookmarkLookup[fileName] {
            if pages.contains(currentPage) {
                pages.remove(currentPage)
            } else {
                pages.insert(currentPage)
            }
            bookmarkLookup[fileName] = pages
        } else {
            bookmarkLookup[fileName] = [currentPage]
        }
    }
}

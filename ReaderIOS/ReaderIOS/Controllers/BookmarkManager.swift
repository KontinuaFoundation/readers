//
//  BookmarkManager.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import Combine
import Foundation

final class BookmarkManager: ObservableObject {
    @Published var bookmarkLookup: [Int: Set<Int>]
    
    init(){
        bookmarkLookup = StateRestoreManager.shared.loadBookmarks()
    }

    /// Checks if the current page is bookmarked for the given file.
    func isBookmarked(workbook: Workbook?, currentPage: Int) -> Bool {
        guard let id = workbook?.id else { return false }
        return bookmarkLookup[id]?.contains(currentPage) ?? false
    }

    /// Toggles the bookmark status for the given file and page.
    func toggleBookmark(for workbook: Workbook?, currentPage: Int) {
        guard let id = workbook?.id else { return }

        var pages = bookmarkLookup[id] ?? []
        if pages.contains(currentPage) {
          pages.remove(currentPage)
        } else {
          pages.insert(currentPage)
        }
        bookmarkLookup[id] = pages
        
        StateRestoreManager.shared.saveBookmarks(bookmarkLookup)
    }
}

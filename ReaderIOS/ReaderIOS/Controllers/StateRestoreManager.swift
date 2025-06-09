//
//  StateRestoreManager.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/5/25.
//

import Foundation

final class StateRestoreManager {
    // MARK: - Singleton Instance

    static let shared = StateRestoreManager()

    // MARK: - UserDefaults Keys

    private let lastWorkbookKey = "lastWorkbook"
    private let workbookPagesKey = "workbookPages" // Dictionary key
    private let bookmarksKey = "bookmarkLookup"

    // Private initializer prevents external instantiation.
    private init() {}

    // MARK: - Save State

    /// Saves the current workbook identifier and its corresponding page number.
    func saveState(workbookID: Int, pageNumber: Int) {
        let defaults = UserDefaults.standard

        defaults.set(workbookID, forKey: lastWorkbookKey)

        var workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [String: Int] ?? [String: Int]()

        workbookPages[String(workbookID)] = pageNumber

        defaults.set(workbookPages, forKey: workbookPagesKey)
    }

    // MARK: - Load State

    /// Loads the last saved workbook identifier and its corresponding page number.
    ///
    /// - Returns: A tuple containing the workbook identifier and page number,
    ///            or `nil` if no state was saved.
    func loadState() -> (workbookID: Int, pageNumber: Int)? {
        let defaults = UserDefaults.standard

        // Retrieve the last opened workbook ID as an Int
        guard let workbookID = defaults.object(forKey: lastWorkbookKey) as? Int else {
            return nil
        }

        return (workbookID, loadPageNumber(for: workbookID))
    }

    /// Loads the saved page number for a specific workbook.
    ///
    /// - Parameter workbookID: The identifier of the workbook.
    /// - Returns: The saved page number, or nil if not found.
    func loadPageNumber(for workbookID: Int) -> Int {
        let defaults = UserDefaults.standard

        if let workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [String: Int],
           let page = workbookPages[String(workbookID)]
        {
            return page
        }

        return 0
    }
    
    /// Persist the in-memory bookmark lookup.
    func saveBookmarks(_ lookup: [Int: Set<Int>]) {
        let defaults = UserDefaults.standard
        
        // Convert [Int:Set<Int>] → [String:[Int]] so it's a plist-compatible value
        let plistFriendly: [String: [Int]] = Dictionary(
            uniqueKeysWithValues: lookup.map { workbookID, pages in
                (String(workbookID), Array(pages))
            }
        )
        defaults.set(plistFriendly, forKey: bookmarksKey)
    }
    
    func loadBookmarks() -> [Int: Set<Int>] {
        let defaults = UserDefaults.standard
        
        guard let saved = defaults.dictionary(forKey: bookmarksKey) as? [String: [Int]] else {
          return [:]
        }
        
        // Convert back to [Int: Set<Int>]
        return Dictionary(
          uniqueKeysWithValues: saved.compactMap { key, array in
            guard let id = Int(key) else { return nil }
            return (id, Set(array))
          }
        )
    }
}

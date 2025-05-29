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
    private let collectionMajorVersionKey = "collectionMajorVersion"
    private let collectionMinorVersionKey = "collectionMinorVersion"

    private let defaults = UserDefaults.standard

    // Private initializer prevents external instantiation.
    private init() {}

    // MARK: - Save State

    /// Saves the current workbook identifier and its corresponding page number.
    func saveState(workbookID: Int, pageNumber: Int) {
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
        if let workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [String: Int],
           let page = workbookPages[String(workbookID)]
        {
            return page
        }

        return 0
    }

    func saveCollectionVersion(major: Int, minor: Int) {
        defaults.set(major, forKey: collectionMajorVersionKey)
        defaults.set(minor, forKey: collectionMinorVersionKey)
    }

    func isCollectionCurrent(latestMajor: Int, latestMinor: Int) -> Bool {
        guard let savedMajor = defaults.object(forKey: collectionMajorVersionKey) as? Int,
              let savedMinor = defaults.object(forKey: collectionMinorVersionKey) as? Int
        else {
            return false
        }

        return savedMajor == latestMajor && savedMinor <= latestMinor
    }
}

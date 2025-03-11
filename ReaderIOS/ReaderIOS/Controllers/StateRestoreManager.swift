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

        // Retrieve the dictionary of workbook page numbers.
        if let workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [Int: Int],
           let pageNumber = workbookPages[workbookID]
        {
            return (workbookID, pageNumber)
        } else {
            return (workbookID, 0)
        }
    }

    /// Loads the saved page number for a specific workbook.
    ///
    /// - Parameter workbookID: The identifier of the workbook.
    /// - Returns: The saved page number, or nil if not found.
    func loadPageNumber(for workbookID: Int) -> Int {
        let defaults = UserDefaults.standard
        if let workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [Int: Int],
           let page = workbookPages[workbookID]
        {
            return page
        }
        return 0
    }
}

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
    func saveState(workbookID: String, pageNumber: Int) {
        let defaults = UserDefaults.standard

        // Save the last opened workbook.
        defaults.set(workbookID, forKey: lastWorkbookKey)

        // Retrieve the current dictionary of workbook page numbers, or create one if it doesn't exist.
        var workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [String: Int] ?? [String: Int]()
        workbookPages[workbookID] = pageNumber

        // Save the updated dictionary back to UserDefaults.
        defaults.set(workbookPages, forKey: workbookPagesKey)
    }

    // MARK: - Load State

    /// Loads the last saved workbook identifier and its corresponding page number.
    ///
    /// - Returns: A tuple containing the workbook identifier and page number,
    ///            or `nil` if no state was saved.
    func loadState() -> (workbookID: String, pageNumber: Int)? {
        let defaults = UserDefaults.standard

        // Retrieve the last opened workbook ID.
        guard let workbookID = defaults.string(forKey: lastWorkbookKey) else {
            return nil
        }

        // Retrieve the dictionary of workbook page numbers.
        if let workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [String: Int],
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
    func loadPageNumber(for workbookID: String) -> Int {
        let defaults = UserDefaults.standard
        if let workbookPages = defaults.dictionary(forKey: workbookPagesKey) as? [String: Int],
           let pg = workbookPages[workbookID]
        {
            return pg
        }
        return 0
    }
}

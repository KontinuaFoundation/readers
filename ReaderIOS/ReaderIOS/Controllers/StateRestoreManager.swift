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
    private let workbookKey = "lastWorkbook"
    private let pageKey = "lastPage"
    
    // Private initializer prevents external instantiation.
    private init() {}
    
    // MARK: - Save State
    /// Saves the current document identifier and page number to UserDefaults.
    func saveState(workbookID: String, pageNumber: Int) {
        let defaults = UserDefaults.standard
        defaults.set(workbookID, forKey: workbookKey)
        defaults.set(pageNumber, forKey: pageKey)
    }
    
    // MARK: - Load State
    /// Loads the last saved document identifier and page number from UserDefaults.
    ///
    /// - Returns: A tuple containing the document identifier and page number,
    ///            or `nil` if no state was saved.
    func loadState() -> (workbookID: String, pageNumber: Int)? {
        let defaults = UserDefaults.standard
        
        // Ensure a valid documentID exists
        guard let workbookID = defaults.string(forKey: workbookKey) else {
            return nil
        }
        
        // Use defaults.integer(forKey:) which returns 0 if the key doesn't exist.
        let pageNumber = defaults.integer(forKey: pageKey)
        return (workbookID, pageNumber)
    }
}

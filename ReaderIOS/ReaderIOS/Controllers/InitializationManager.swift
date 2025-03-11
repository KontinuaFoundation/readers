//
//  InitializationManager.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import Foundation
import PDFKit
import SwiftUI

enum FetchConstants {
    static let maxAttempts = 6
    static let attemptDelay = 1000
}

final class InitializationManager: ObservableObject {
    @Published var isInitialized = false
    @Published var loadFailed = false
    @Published var workbooks: [WorkbookPreview] = []
    @Published var pdfDocument: PDFDocument?
    @Published var workbookID: Int?
    @Published var latestCollection: Collection?
    @Published var workbook: Workbook?
    @Published var attempts: Int = 0
    @Published var delay: Int = 0

    init() {
        loadInitialData()
    }

    func loadInitialData(delay: Int = 0) {
        self.delay = delay
        attempts += 1
        fetchLatestCollection()
    }

    private func fetchLatestCollection() {
        NetworkingService.shared.fetchLatestCollection { [weak self] result in
            switch result {
            case let .success(collection):
                DispatchQueue.main.async {
                    self?.latestCollection = collection
                    self?.fetchWorkbookList(collection: collection)
                }
            case let .failure(error):
                print("Failed to fetch latest collection: \(error)")
            }
        }
    }

    private func fetchWorkbookList(collection: Collection) {
        let start = DispatchTime.now()

        NetworkingService.shared.fetchWorkbooks(collection: collection) { [weak self] result in
            switch result {
            case let .success(workbooks):
                DispatchQueue.main.async {
                    self?.workbooks = workbooks
                    if let savedState = StateRestoreManager.shared.loadState() {
                        if let open = workbooks.first(where: { $0.id == savedState.workbookID }) {
                            self?.workbookID = savedState.workbookID
                            self?.fetchWorkbook(for: open.id)
                        }
                    } else {
                        self?.workbookID = workbooks.first?.id
                        if let workbookID = self?.workbookID {
                            self?.fetchWorkbook(for: workbookID)
                            self?.isInitialized = true
                        } else {
                            print("workbook list returned empty list, this should never happen.")
                            self?.loadFailed = true
                        }
                    }
                }
            case let .failure(error):
                print("Error fetching workbooks: \(error)")

                // if delay is set, does not show failure until after delay is elapsed
                DispatchQueue.main.asyncAfter(deadline: start + .milliseconds(self?.delay ?? 0)) {
                    self?.loadFailed = true
                }
            }
        }
    }

    private func fetchWorkbook(for id: Int) {
        NetworkingService.shared.fetchWorkbook(id: id) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case let .success(workbook):
                    self?.workbook = workbook
                    self?.fetchPDF(for: workbook)
                case let .failure(error):
                    print("Error fetching PDF: \(error)")
                }
                // Mark initialization complete regardless; you could also have error states.
                self?.isInitialized = true
            }
        }
    }

    private func fetchPDF(for workbook: Workbook) {
        NetworkingService.shared.fetchPDF(workbook: workbook) { [weak self] result in
            switch result {
            case let .success(pdf):
                self?.pdfDocument = pdf
            case let .failure(error):
                print("Error fetching PDF: \(error)")
            }
        }
    }
}

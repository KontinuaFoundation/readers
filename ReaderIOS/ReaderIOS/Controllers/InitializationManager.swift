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
    @Published var workbooks: [Workbook] = []
    @Published var pdfDocument: PDFDocument?
    @Published var attempts: Int = 0

    init() {
        loadInitialData()
    }

    func loadInitialData(delay: Int = 0) {
        let start = DispatchTime.now()
        self.attempts += 1

        NetworkingService.shared.fetchWorkbooks { [weak self] result in
            switch result {
            case let .success(workbooks):
                DispatchQueue.main.async {
                    self?.workbooks = workbooks
                    if let firstWorkbook = workbooks.first {
                        self?.fetchPDF(for: firstWorkbook.pdfName)
                    } else {
                        self?.isInitialized = true
                    }
                }
            case let .failure(error):
                print("Error fetching workbooks: \(error)")

                // if delay is set, does not show failure until after delay is elapsed
                DispatchQueue.main.asyncAfter(deadline: start + .milliseconds(delay)) {
                    self?.loadFailed = true
                }
            }
        }
    }

    private func fetchPDF(for fileName: String) {
        NetworkingService.shared.fetchPDF(fileName: fileName) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case let .success(pdf):
                    self?.pdfDocument = pdf
                case let .failure(error):
                    print("Error fetching PDF: \(error)")
                }
                // Mark initialization complete regardless; you could also have error states.
                self?.isInitialized = true
            }
        }
    }
}

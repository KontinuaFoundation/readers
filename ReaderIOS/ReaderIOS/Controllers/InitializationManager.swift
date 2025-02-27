//
//  InitializationManager.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import Foundation
import PDFKit
import SwiftUI

final class InitializationManager: ObservableObject {
    @Published var isInitialized = false
    @Published var loadFailed = false
    @Published var workbooks: [Workbook] = []
    @Published var pdfDocument: PDFDocument?

    // Optionally load other data (chapters, covers, etc.) as needed

    init() {
        loadInitialData()
    }

    func loadInitialData() {
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
                // Handle error appropriately
                DispatchQueue.main.async {
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

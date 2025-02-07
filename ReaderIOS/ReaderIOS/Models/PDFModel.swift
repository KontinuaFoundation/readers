//
//  PDFModel.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import Foundation
import PDFKit
import SwiftUI

final class PDFViewModel: ObservableObject {
    @Published var fileName: String?
    @Published var currentPage: Int = 0
    @Published var bookmarkLookup: [String: Set<Int>] = [:]
    @Published var pdfDocument: PDFDocument?
    @Published var pagePaths: [String: [(path: Path, color: Color)]] = [:]
    @Published var highlightPaths: [String: [(path: Path, color: Color)]] = [:]

    var isCurrentPageBookmarked: Bool {
        guard let fileName = fileName else { return false }
        return bookmarkLookup[fileName]?.contains(currentPage) ?? false
    }

    func loadPDFFromURL() {
        guard let fileName = fileName else { return }
        Task {
            do {
                let document = try await PDFNetworking.fetchPDF(fileName: fileName)
                await MainActor.run {
                    self.pdfDocument = document
                }
            } catch {
                print("Error downloading PDF: \(error.localizedDescription)")
            }
        }
    }

    func loadPathsForPage(_ pageIndex: Int) {
        let key = uniqueKey(for: pageIndex)
        if pagePaths[key] == nil {
            pagePaths[key] = []
        }
        if highlightPaths[key] == nil {
            highlightPaths[key] = []
        }
    }

    func uniqueKey(for pageIndex: Int) -> String {
        guard let fileName = fileName else { return "\(pageIndex)" }
        return "\(fileName)-\(pageIndex)"
    }

    func clearMarkup() {
        let key = uniqueKey(for: currentPage)
        
        // Remove all paths for the current page
        pagePaths[key] = []
        highlightPaths[key] = []
        
        // Make sure changes are published
        objectWillChange.send()
        
        // Clear any cached data for this page
        loadPathsForPage(currentPage)
    }

    func toggleCurrentPageInBookmarks() {
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

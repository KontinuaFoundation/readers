//
//  PDFSearchHighlighter.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 1/15/25.
//

import PDFKit

class PDFSearchHighlighter: ObservableObject {
    private let pdfDoc: PDFDocument
    private var currentHighlights: [PDFAnnotation] = []

    init(pdfDoc: PDFDocument) {
        self.pdfDoc = pdfDoc
    }

    /// Highlight search result on a specific page
    func highlightSearchResult(searchTerm: String, onPage pageNumber: Int, color: UIColor = .yellow) {
        guard let page = pdfDoc.page(at: pageNumber) else { return }

        // Get the full page text
        guard let pageContent = page.string?.lowercased() else { return }

        // Get search terms
        let searchTerms = searchTerm.lowercased()
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }

        // Find the location of the search terms in the page content
        if let range = findSearchTermRange(searchTerms: searchTerms, in: pageContent) {
            // Convert string range to PDF selection
            if let selection = page.selection(for: range) {
                // Create highlight annotation
                let highlight = PDFAnnotation(bounds: selection.bounds(for: page),
                                              forType: .highlight,
                                              withProperties: nil)

                highlight.color = color.withAlphaComponent(0.7)

                page.addAnnotation(highlight)

                // Store reference to highlight for later removal
                currentHighlights.append(highlight)
            }
        }
    }

    /// Remove all current highlights
    func clearHighlights() {
        for highlight in currentHighlights {
            if let page = highlight.page {
                page.removeAnnotation(highlight)
            }
        }
        currentHighlights.removeAll()
    }

    /// Find the range of the search terms in the page content
    private func findSearchTermRange(searchTerms: [String], in pageContent: String) -> NSRange? {
        // For single term search
        if searchTerms.count == 1 {
            let searchTerm = searchTerms[0]
            return (pageContent as NSString).range(
                of: searchTerm,
                options: .caseInsensitive
            )
        }

        // For multi-term search
        let searchPhrase = searchTerms.joined(separator: " ")
        return (pageContent as NSString).range(
            of: searchPhrase,
            options: .caseInsensitive
        )
    }
}

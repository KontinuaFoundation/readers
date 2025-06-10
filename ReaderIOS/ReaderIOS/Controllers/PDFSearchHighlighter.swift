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

    func highlightSearchResult(searchTerm: String,
                               onPage pageNumber: Int,
                               color: UIColor = .yellow)
    {
        guard let page = pdfDoc.page(at: pageNumber),
              let text = page.string else { return }

        // Split into terms, escape regex metacharacters
        let terms = searchTerm
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }
            .map { NSRegularExpression.escapedPattern(for: $0) }

        // Build a pattern that matches any whitespace (including newline)
        let pattern = terms.joined(separator: "\\s+")
        guard let regex = try? NSRegularExpression(
            pattern: pattern,
            options: [.caseInsensitive]
        ) else { return }

        let nsText = text as NSString
        let fullRange = NSRange(location: 0, length: nsText.length)

        // Find all matches
        let matches = regex.matches(in: text, options: [], range: fullRange)
        for match in matches {
            if let selection = page.selection(for: match.range) {
                // Break into per-line pieces so highlights don't bleed across lines
                for lineSel in selection.selectionsByLine() {
                    let highlight = PDFAnnotation(
                        bounds: lineSel.bounds(for: page),
                        forType: .highlight,
                        withProperties: nil
                    )
                    highlight.color = color.withAlphaComponent(0.7)
                    page.addAnnotation(highlight)
                    currentHighlights.append(highlight)
                }
            }
        }
    }

    func clearHighlights() {
        for cur in currentHighlights {
            cur.page?.removeAnnotation(cur)
        }
        currentHighlights.removeAll()
    }
}

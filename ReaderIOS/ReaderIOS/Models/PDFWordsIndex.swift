//
//  PDFWordsIndex.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 12/11/24.
//

import PDFKit

class PDFWordsIndex: ObservableObject {
    // Dictionary mapping words to the pages they appear on
    @Published private var wordToPages: [String: Set<Int>] = [:]

    // Original page texts for reference
    @Published private var pageTexts: [Int: String] = [:]

    // Tokenized page text for context retrieval
    @Published private var pageTokens: [Int: [String]] = [:]

    func indexPDF(from pdf: PDFDocument) {
        // Clear existing index when loading new document
        wordToPages.removeAll()
        pageTexts.removeAll()
        pageTokens.removeAll()

        for pageIndex in 0 ..< pdf.pageCount {
            guard let page = pdf.page(at: pageIndex) else { continue }
            guard let pageContent = page.attributedString else { continue }

            let plainText = pageContent.string
            let pageNumber = pageIndex

            // Store original page text
            pageTexts[pageNumber] = plainText

            // Tokenize
            let words = plainText.lowercased()
                .components(separatedBy: .whitespacesAndNewlines)
                .filter { !$0.isEmpty }

            pageTokens[pageNumber] = words

            // Index words
            for word in words {
                wordToPages[word, default: []].insert(pageNumber)
            }
        }
    }

    /// Search method to find pages containing all of the search terms,
    /// and return contextual snippets as well.
    ///
    /// The returned dictionary maps page numbers to a snippet of text
    /// around the first occurrence of the search terms on that page.
    ///
    /// If multiple search terms are provided, we look for the first place
    /// on the page where they appear in sequence. If any term cannot be
    /// found in sequence, that page is skipped.
    func search(for term: String, contextWindow: Int = 2) -> [Int: String] {
        let searchTerms = term.lowercased()
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }

        guard !searchTerms.isEmpty else { return [:] }

        // Get matches for first term
        var resultPages = wordToPages[searchTerms[0]] ?? []

        // Intersect with matches for remaining terms
        for t in searchTerms.dropFirst() {
            resultPages = resultPages.intersection(wordToPages[t] ?? [])
        }

        var pageSnippets: [Int: String] = [:]

        // For each page in results, find the first occurrence of the terms in sequence
        for pageNumber in resultPages {
            guard let tokens = pageTokens[pageNumber] else { continue }

            // If there's only one term, just find that term
            if searchTerms.count == 1, let idx = tokens.firstIndex(of: searchTerms[0]) {
                let snippet = makeSnippet(tokens: tokens, indexRange: idx ... idx, window: contextWindow)
                pageSnippets[pageNumber] = snippet
            } else {
                // Multiple terms: we need to find a sequence
                if let range = findSequence(in: tokens, sequence: searchTerms) {
                    // Construct a snippet around this range
                    let snippet = makeSnippet(tokens: tokens, indexRange: range, window: contextWindow)
                    pageSnippets[pageNumber] = snippet
                }
            }
        }

        return pageSnippets
    }

    // Utility to create a snippet around a given range of tokens
    private func makeSnippet(tokens: [String], indexRange: ClosedRange<Int>, window: Int) -> String {
        let startIndex = max(indexRange.lowerBound - window, 0)
        let endIndex = min(indexRange.upperBound + window, tokens.count - 1)
        let snippetTokens = tokens[startIndex ... endIndex]
        return snippetTokens.joined(separator: " ")
    }

    // Find the first occurrence of a sequence of terms in tokens
    // This checks if the terms appear in order and contiguously.
    private func findSequence(in tokens: [String], sequence: [String]) -> ClosedRange<Int>? {
        guard sequence.count <= tokens.count else { return nil }

        // A naive approach: scan tokens to find where sequence starts
        for i in 0 ... (tokens.count - sequence.count) {
            let slice = tokens[i ..< i + sequence.count]
            if Array(slice) == sequence {
                return i ... (i + sequence.count - 1)
            }
        }

        return nil
    }

    // Get text for a specific page
    func getText(for page: Int) -> String? {
        pageTexts[page]
    }

    // Get all indexed text
    func getAllPageTexts() -> [Int: String] {
        pageTexts
    }
}

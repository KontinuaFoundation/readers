import PDFKit

class PDFWordsIndex: ObservableObject {
    // Dictionary mapping words to the pages they appear on
    @Published private var wordToPages: [String: Set<Int>] = [:]

    // Original page texts for reference
    @Published private var pageTexts: [Int: String] = [:]

    // Tokenized page text for searching
    @Published private var pageTokens: [Int: [String]] = [:]

    // Store token ranges to map back to original text
    @Published private var tokenRanges: [Int: [Range<String.Index>]] = [:]

    func indexPDF(from pdf: PDFDocument) {
        wordToPages.removeAll()
        pageTexts.removeAll()
        pageTokens.removeAll()
        tokenRanges.removeAll()

        for pageIndex in 0 ..< pdf.pageCount {
            guard let page = pdf.page(at: pageIndex) else { continue }
            guard let pageContent = page.attributedString else { continue }

            let plainText = pageContent.string
            let pageNumber = pageIndex

            // Store original page text
            pageTexts[pageNumber] = plainText

            // Create scanner for tokenization while preserving ranges
            var ranges: [Range<String.Index>] = []
            var tokens: [String] = []

            let scanner = Scanner(string: plainText)
            scanner.charactersToBeSkipped = .whitespacesAndNewlines

            while !scanner.isAtEnd {
                if let token = scanner.scanCharacters(from: .alphanumerics) {
                    let startIndex = plainText.index(
                        plainText.startIndex,
                        offsetBy: scanner.currentIndex.utf16Offset(in: plainText) - token.count
                    )
                    let endIndex = plainText.index(
                        plainText.startIndex,
                        offsetBy: scanner.currentIndex.utf16Offset(in: plainText)
                    )
                    ranges.append(startIndex ..< endIndex)
                    tokens.append(token.lowercased())
                } else {
                    // Skip non-alphanumeric character
                    _ = scanner.scanCharacter()
                }
            }

            // Store token ranges and lowercase tokens
            tokenRanges[pageNumber] = ranges
            pageTokens[pageNumber] = tokens

            // Index words
            for token in tokens {
                wordToPages[token, default: []].insert(pageNumber)
            }
        }
    }

    func search(for term: String, contextWindow: Int = 2) -> [Int: String] {
        let searchTerms = term.lowercased()
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }

        guard !searchTerms.isEmpty else { return [:] }

        var resultPages = wordToPages[searchTerms[0]] ?? []

        for term in searchTerms.dropFirst() {
            resultPages = resultPages.intersection(wordToPages[term] ?? [])
        }

        var pageSnippets: [Int: String] = [:]

        for pageNumber in resultPages {
            guard let tokens = pageTokens[pageNumber],
                  let ranges = tokenRanges[pageNumber],
                  let originalText = pageTexts[pageNumber] else { continue }

            if searchTerms.count == 1,
               let idx = tokens.firstIndex(of: searchTerms[0])
            {
                let snippet = makeSnippet(
                    originalText: originalText,
                    tokenRanges: ranges,
                    matchRange: idx ... idx,
                    window: contextWindow
                )
                pageSnippets[pageNumber] = snippet
            } else {
                if let range = findSequence(in: tokens, sequence: searchTerms) {
                    let snippet = makeSnippet(
                        originalText: originalText,
                        tokenRanges: ranges,
                        matchRange: range,
                        window: contextWindow
                    )
                    pageSnippets[pageNumber] = snippet
                }
            }
        }

        return pageSnippets
    }

    private func makeSnippet(
        originalText: String,
        tokenRanges: [Range<String.Index>],
        matchRange: ClosedRange<Int>,
        window: Int
    ) -> String {
        let startTokenIdx = max(matchRange.lowerBound - window, 0)
        let endTokenIdx = min(matchRange.upperBound + window, tokenRanges.count - 1)

        // Get the text range from the first token to the last token
        let snippetStart = tokenRanges[startTokenIdx].lowerBound
        let snippetEnd = tokenRanges[endTokenIdx].upperBound

        // Extract the original text with proper formatting
        return String(originalText[snippetStart ..< snippetEnd])
            .replacingOccurrences(of: "\n", with: " ")
            .components(separatedBy: .newlines)
            .joined(separator: " ")
    }

    private func findSequence(in tokens: [String], sequence: [String]) -> ClosedRange<Int>? {
        guard sequence.count <= tokens.count else { return nil }

        for index in 0 ... (tokens.count - sequence.count) {
            let slice = tokens[index ..< (index + sequence.count)]
            if Array(slice) == sequence {
                return index ... (index + sequence.count - 1)
            }
        }

        return nil
    }

    func getText(for page: Int) -> String? {
        pageTexts[page]
    }

    func getAllPageTexts() -> [Int: String] {
        pageTexts
    }
}

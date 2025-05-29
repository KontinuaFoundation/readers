//
//  ChapterSearch.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 12/11/24.
//

import Foundation
import SwiftUI

// ChapterSearchable protocol to make searching more generic
protocol ChapterSearchable {
    var searchableText: String { get }
    var pageNumber: Int { get }
}

extension Chapter: ChapterSearchable {
    var searchableText: String {
        title
    }

    var pageNumber: Int {
        startPage - 1
    }
}

// Search result with highlighting information
struct SearchResult<T: ChapterSearchable> {
    let item: T
    let highlightRanges: [Range<String.Index>]
}

// Search utility struct
enum ChapterSearch {
    /// Filters chapters based on a search text with match highlighting
    /// - Parameters:
    ///   - chapters: The array of chapters to filter
    ///   - searchText: The text to search for
    /// - Returns: Filtered array of search results with highlight information
    static func filter<T: ChapterSearchable>(
        _ chapters: [T]?,
        by searchText: String
    ) -> [SearchResult<T>] {
        guard let chapters = chapters, !searchText.isEmpty else {
            return chapters?.map { SearchResult(item: $0, highlightRanges: []) } ?? []
        }

        return chapters.compactMap { chapter in
            // Find all case-insensitive matches
            let lowercaseText = chapter.searchableText.lowercased()
            let lowercaseSearch = searchText.lowercased()

            // Check if search text is contained
            guard lowercaseText.contains(lowercaseSearch) else {
                return nil
            }

            // Find all matching ranges
            var ranges: [Range<String.Index>] = []
            var searchStart = chapter.searchableText.startIndex

            while let range = chapter.searchableText[searchStart...].range(of: searchText, options: .caseInsensitive) {
                ranges.append(range)
                searchStart = range.upperBound

                // Prevent infinite loop
                if searchStart >= chapter.searchableText.endIndex {
                    break
                }
            }

            return SearchResult(
                item: chapter,
                highlightRanges: ranges
            )
        }
    }
}

// Extension to create a highlighted text view
extension SearchResult where T == Chapter {
    /// Creates a view with the chapter title, highlighting matching text
    func highlightedTitleView() -> some View {
        VStack(alignment: .leading) {
            Text("Chapter \(item.chapNum)")
                .font(.caption)

            Text(highlightRanges.isEmpty
                ? highlightAttributedString()
                : AttributedString(item.title))
        }
    }

    /// Creates an attributed string with matching text bolded
    private func highlightAttributedString() -> AttributedString {
        var attributedString = AttributedString(item.title)

        // Apply bold to matching ranges
        for range in highlightRanges {
            // Convert String.Index to String
            let matchedSubstring = String(item.title[range])

            // Find the range in the AttributedString
            if let foundRange = attributedString.range(of: matchedSubstring) {
                attributedString[foundRange].font = .body.bold()
            }
        }

        return attributedString
    }
}

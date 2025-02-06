//
//  ContentView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import PDFKit
import SwiftUI

struct ContentView: View {
    @Binding var isShowingBookmarks: Bool
    @Binding var searchText: String
    @Binding var chapters: [Chapter]?
    @Binding var currentPage: Int
    @Binding var columnVisibility: NavigationSplitViewVisibility
    let wordSearchResults: [(page: Int, snippet: String)]
    @Binding var bookmarkLookup: [String: Set<Int>]
    @Binding var currentPdfFileName: String?

    var filteredChapters: [SearchResult<Chapter>] {
        ChapterSearch.filter(chapters, by: searchText)
    }

    // MARK: - Body

    var body: some View {
        Group {
            if !isShowingBookmarks {
                VStack {
                    SearchBar(text: $searchText, onClear: {
                        // Handle clear action
                    })
                    .padding(.horizontal)

                    if chapters != nil {
                        chaptersListView
                    } else {
                        ProgressView()
                    }
                }
            } else {
                bookmarksView
            }
        }
        .toolbar {
            ToolbarItem(placement: .automatic) {
                Toggle(isOn: $isShowingBookmarks) {
                    Image(systemName: isShowingBookmarks ? "bookmark.fill" : "bookmark")
                        .foregroundColor(.accentColor)
                }
                .toggleStyle(.button)
                .buttonStyle(.plain)
                .accessibilityLabel(isShowingBookmarks ? "Show All Chapters" : "Show Bookmarked Chapters")
            }
        }
    }

    // MARK: - Subviews
    private var chaptersListView: some View {
        List {
            chapterSection
            if searchText.count > 1 {
                wordMatchesSection
            }
        }
    }

    private var chapterSection: some View {
        Section(header: Text("Chapters: ")) {
            if filteredChapters.isEmpty, !searchText.isEmpty {
                Text("No chapters found")
                    .foregroundColor(.gray)
            } else {
                ForEach(filteredChapters, id: \.item.pageNumber) { searchResult in
                    searchResult.highlightedTitleView()
                        .tag(searchResult.item.pageNumber)
                        .onTapGesture { currentPage = searchResult.item.pageNumber }
                }
            }
        }
    }

    private var wordMatchesSection: some View {
        Section(header: Text("Word Matches:")) {
            if wordSearchResults.isEmpty {
                Text("No word matches found")
                    .foregroundColor(.gray)
            } else {
                ForEach(wordSearchResults, id: \.page) { result in
                    VStack(alignment: .leading) {
                        Text(result.snippet)
                        Text("Page \(result.page + 1)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .onTapGesture {
                        currentPage = result.page
                        columnVisibility = .detailOnly
                    }
                }
            }
        }
    }

    private var bookmarksView: some View {
        Group {
            if let currentPdfFileName = currentPdfFileName,
               let bookmarks = bookmarkLookup[currentPdfFileName]
            {
                List(Array(bookmarks).sorted(), id: \.self) { bookmark in
                    HStack {
                        Text("Page \(bookmark + 1)")
                        Spacer()
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        currentPage = bookmark
                    }
                }
            } else {
                Text("No bookmarks available")
                    .font(.callout)
                    .foregroundColor(.gray)
            }
        }
    }
}

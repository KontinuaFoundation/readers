//
//  SearchView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import PDFKit
import SwiftUI

struct SearchView: View {
    // Bindings from the parent for interactions that affect navigation
    @Binding var currentPage: Int
    @Binding var columnVisibility: NavigationSplitViewVisibility

    // Local search state – now maintained in SearchView
    @State private var searchText: String = ""
    @State private var wordsIndex = PDFWordsIndex()
    @State private var searchHighlighter: PDFSearchHighlighter?

    // Data passed from the parent
    var chapters: [Chapter]?
    var fetchWorkbookAndChapters: () -> Void
    var pdfDocument: PDFDocument?

    // Observing networking service for blurring
    @ObservedObject private var networkingSingleton = NetworkingService.shared

    // Computed properties using the local searchText and wordsIndex
    private var filteredChapters: [SearchResult<Chapter>] {
        ChapterSearch.filter(chapters, by: searchText)
    }

    private var wordSearchResults: [(page: Int, snippet: String)] {
        guard !searchText.isEmpty else { return [] }
        let pageResults = wordsIndex.search(for: searchText)
        return pageResults.sorted { $0.key < $1.key }
            .map { (page: $0.key, snippet: $0.value) }
    }

    func indexPDFDocument() {
        if let currentPDF = pdfDocument {
            wordsIndex.indexPDF(from: currentPDF)
            searchHighlighter?.clearHighlights()
            searchHighlighter = PDFSearchHighlighter(pdfDoc: currentPDF)
        }
    }

    var body: some View {
        VStack {
            // SearchBar uses the local searchText binding.
            SearchBar(text: $searchText, onClear: {
                searchHighlighter?.clearHighlights()
            })
            .padding(.horizontal)
            // List displaying chapters and word matches
            if chapters != nil {
                Group {
                    List(selection: Binding(
                        get: { currentPage },
                        set: { newPage in
                            if let page = newPage {
                                currentPage = page
                                columnVisibility = .detailOnly
                            }
                        }
                    )) {
                        Section(header: Text("Chapters")) {
                            if filteredChapters.isEmpty, !searchText.isEmpty {
                                Text("No chapters found")
                                    .foregroundColor(.gray)
                            } else {
                                ForEach(filteredChapters, id: \.item.pageNumber) { searchResult in
                                    searchResult.highlightedTitleView()
                                        .tag(searchResult.item.pageNumber)
                                }
                            }
                        }

                        if searchText.count > 1 {
                            Section(header: Text("Word Matches")) {
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
                                            print("Highlighting \(result.snippet) on page \(result.page + 1)")
                                            searchHighlighter?.clearHighlights()
                                            searchHighlighter?.highlightSearchResult(
                                                searchTerm: result.snippet,
                                                onPage: result.page
                                            )
                                            columnVisibility = .detailOnly
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .blur(radius: networkingSingleton.isContentLoading ? 10 : 0)
            } else {
                ProgressView()
                    .onAppear(perform: fetchWorkbookAndChapters)
            }
        }
        .onAppear {
            indexPDFDocument()
        }
        .onChange(of: pdfDocument) { _, _ in
            indexPDFDocument()
        }
    }
}

struct SearchBar: View {
    @Binding var text: String
    var onClear: (() -> Void)? // Optional closure to be called on clear

    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)

            TextField("Search chapters and words", text: $text)
                .textFieldStyle(PlainTextFieldStyle())
                .disableAutocorrection(true)

            if !text.isEmpty {
                Button(action: {
                    text = "" // Clear the text
                    onClear?() // Call the optional clear function if it exists
                }, label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                })
            }
        }
        .padding(8)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

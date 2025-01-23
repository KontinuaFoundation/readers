//
//  TableOfContentsSplitView.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 11/10/24.
//
import PDFKit
import SwiftUI

struct Chapter: Identifiable, Codable {
    let id: String
    let title: String
    let book: String
    let chapNum: Int
    let covers: [Cover]
    let startPage: Int
    let requires: [String]?

    enum CodingKeys: String, CodingKey {
        case id, title, book, covers, requires
        case chapNum = "chap_num"
        case startPage = "start_page"
    }
}

struct Cover: Identifiable, Codable {
    let id: String
    let desc: String
    let videos: [Video]?
    let references: [Reference]?
}

struct Video: Identifiable, Codable {
    var id = UUID()
    let link: String
    let title: String

    enum CodingKeys: String, CodingKey {
        case link, title
    }
}

struct Reference: Identifiable, Codable {
    var id = UUID()
    let link: String
    let title: String

    enum CodingKeys: String, CodingKey {
        case link, title
    }
}

struct Workbook: Codable, Hashable, Identifiable {
    let id: String
    let metaName: String
    let pdfName: String
}

struct NavigationPDFSplitView: View {
    @State private var workbooks: [Workbook]?
    @State private var chapters: [Chapter]?
    @State private var covers: [Cover]?
    @State private var selectedWorkbookID: String?
    @State private var selectedChapterID: String?

    @State private var currentPage: Int = 0
    @State private var currentPdfFileName: String?
    @State private var isShowingBookmarks: Bool = false

    @State private var columnVisibility = NavigationSplitViewVisibility.automatic

    @State private var bookmarkLookup = [String: Set<Int>]()

    // State vars for search
    @State private var pdfDocument: PDFDocument?
    @State private var searchText = ""
    @State private var wordsIndex = PDFWordsIndex()
    
    @State private var chapterManager: ChapterManager?
    @State private var searchHighlighter: PDFSearchHighlighter?

    var filteredChapters: [SearchResult<Chapter>] {
        ChapterSearch.filter(chapters, by: searchText)
    }

    // Compute word search results from wordsIndex
    // Returns pages that contain the searched terms
    var wordSearchResults: [(page: Int, snippet: String)] {
        guard !searchText.isEmpty else { return [] }
        let pageResults = wordsIndex.search(for: searchText)
        // Sort by page number
        return pageResults.sorted { $0.key < $1.key }.map { (page: $0.key, snippet: $0.value) }
    }

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            if let workbooks = workbooks {
                List(workbooks, selection: $selectedWorkbookID) { workbook in
                    HStack {
                        Image(systemName: "icloud.and.arrow.down") // Download icon
                            .font(.caption)
                            .foregroundColor(.blue)
                        Text(workbook.id)
                            .tag(workbook.id)
                    }
                }
            } else {
                ProgressView("Fetching Workbooks")
                    .onAppear {
                        fetchWorkbooks()
                    }
            }
        }
        content: {
            Group {
                if !isShowingBookmarks {
                    VStack {
                        // Add search bar
                        SearchBar(text: $searchText, onClear: {
                            if let searchHighlighter {
                                searchHighlighter.clearHighlights()
                            }
                        })
                        .padding(.horizontal)

                        // Combine chapters and word matches into one list
                        if chapters != nil {
                            List(selection: Binding(
                                get: { currentPage },
                                set: { newPage in
                                    if let page = newPage {
                                        currentPage = page
                                    }
                                }
                            )) {
                                // Chapter search results
                                Section(header: Text("Chapters: ")) {
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

                                // Word matches (appear directly after chapter results)
                                if searchText.count > 1 {
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

                                                .tag(result.page)  // Add tag for selection

 /*                                               .onTapGesture {
                                                    currentPage = result.page

                                                    if let searchHighlighter {
                                                        print(
                                                            "Highlighting \(result.snippet) on page \(result.page + 1)"
                                                        )
                                                        searchHighlighter.clearHighlights()
                                                        searchHighlighter.highlightSearchResult(
                                                            searchTerm: result.snippet,
                                                            onPage: result.page
                                                        )
                                                    }

                                                    columnVisibility = .detailOnly
                                                }*/
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            ProgressView()
                                .onAppear(perform: fetchChapters)
                        }
                    }
                } else {
                    // Bookmarks view
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

        } detail: {
            if currentPdfFileName != nil {
                // TODO: Only give access to bookmarks for current file.
                PDFView(
                    fileName: $currentPdfFileName,
                    currentPage: $currentPage,
                    bookmarkLookup: $bookmarkLookup,
                    covers: $covers,
                    pdfDocument: $pdfDocument
                )
            } else {
                ProgressView("Getting the latest workbook.")
            }
        }
        .onChange(of: selectedWorkbookID) {
            guard let selectedWorkbook = selectedWorkbook else { return }

            if currentPdfFileName != selectedWorkbook.pdfName {
                currentPdfFileName = selectedWorkbook.pdfName
            }

            fetchChapters()
        }
        .onChange(of: currentPage) { _, _ in
            updateSelectedChapter()
        }
        .onChange(of: pdfDocument) { _, newPDFDocument in
            // Move indexing code here
            if let currentPDF = newPDFDocument {
                wordsIndex.indexPDF(from: currentPDF)
                if let searchHighlighter {
                    searchHighlighter.clearHighlights()
                }
                searchHighlighter = PDFSearchHighlighter(pdfDoc: currentPDF)
            }
        }
    }

    var selectedWorkbook: Workbook? {
        workbooks?.first(where: { $0.id == selectedWorkbookID })
    }

    /*
    // TODO: Selected chapter should be based on the current page number.
    var selectedChapter: Chapter? {
        chapters?.first(where: { $0.id == selectedChapterID })
    }*/

    func fetchChapters() {
        guard let fileName = selectedWorkbook?.metaName else {
            return
        }

        guard let url = URL(string: "http://localhost:8000/meta/\(fileName)") else {
            print("Invalid chapter meta URL.")
            return
        }

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let config = URLSessionConfiguration.default
        config.urlCache = nil
        config.requestCachePolicy = .reloadIgnoringLocalCacheData

        let session = URLSession(configuration: config)

        let task = session.dataTask(with: request) { data, _, error in
            if let error = error {
                print("Error fetching chapters: \(error)")
                return
            }
            guard let data = data else {
                print("No data received from URL.")
                return
            }

            do {
                let decoder = JSONDecoder()
                let chapterResponse = try decoder.decode([Chapter].self, from: data)

                DispatchQueue.main.async {
                    chapters = chapterResponse
                    selectedChapterID = chapters?.first?.id
                    setupChapterManager()
                }

            } catch {
                print("Error decoding chapters: \(error)")
            }
        }

        task.resume()
    }

    func fetchWorkbooks() {
        guard let url = URL(string: "http://localhost:8000/workbooks.json") else {
            print("Invalid workbooks URL.")
            return
        }

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let config = URLSessionConfiguration.default
        config.urlCache = nil
        config.requestCachePolicy = .reloadIgnoringLocalCacheData

        let session = URLSession(configuration: config)

        let task = session.dataTask(with: request) { data, _, error in
            if let error = error {
                print("Error fetching workbooks: \(error)")
                return
            }

            guard let data = data else {
                print("No data received from URL.")
                return
            }

            do {
                let decoder = JSONDecoder()
                let workbookResponse = try decoder.decode([Workbook].self, from: data)

                DispatchQueue.main.async {
                    workbooks = workbookResponse
                    if let id = workbooks?.first?.id {
                        selectedWorkbookID = id
                    }
                }
            } catch {
                print("Error decoding workbooks: \(error)")
            }
        }

        task.resume()
    }
    
    func setupChapterManager() {
        if let chapters = chapters {
            let chapterManager = ChapterManager(chapters: chapters)
            // Update selectedChapterID whenever currentPage changes
            self.chapterManager = chapterManager
        }
    }
    
    func updateSelectedChapter() {
        if let chapterManager = chapterManager {
            if let newChapter = chapterManager.getChapter(forPage: currentPage){
                selectedChapterID = newChapter.id
                covers = newChapter.covers
                print("Updated covers: \(covers?.map(\.desc) ?? [])")
            }
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

#Preview {
    NavigationPDFSplitView()
}

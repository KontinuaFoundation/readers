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

    @State private var bookmarkLookup = [String: Set<Int>]()

    // State vars for search
    @State private var pdfDocument: PDFDocument?
    @State private var searchText = ""
    @State private var wordsIndex = PDFWordsIndex()

    var filteredChapters: [SearchResult<Chapter>] {
        ChapterSearch.filter(chapters, by: searchText)
    }

    // Compute word search results from wordsIndex
    // Returns pages that contain the searched terms
    var wordSearchResults: [(page: Int, snippet: String)] {
        guard !searchText.isEmpty else { return [] }
        let pageResults = wordsIndex.search(for: searchText) // Now returns [Int: String]
        // Sort by page number
        return pageResults.sorted { $0.key < $1.key }.map { (page: $0.key, snippet: $0.value) }
    }

    var body: some View {
        NavigationSplitView {
            if let workbooks = workbooks {
                List(workbooks, selection: $selectedWorkbookID) { workbook in
                    Text(workbook.id)
                        .tag(workbook.id)
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
                        SearchBar(text: $searchText)
                            .padding(.horizontal)

                        // Combine chapters and word matches into one list
                        if chapters != nil {
                            List(selection: $selectedChapterID) {
                                // Chapter search results
                                Section(header: Text("Chapters: ")) {
                                    if filteredChapters.isEmpty, !searchText.isEmpty {
                                        Text("No chapters found")
                                            .foregroundColor(.gray)
                                    } else {
                                        ForEach(filteredChapters, id: \.item.id) { searchResult in
                                            searchResult.highlightedTitleView()
                                                .tag(searchResult.item.id)
                                        }
                                    }
                                }

                                // Word matches (appear directly after chapter results)
                                if !searchText.isEmpty {
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
                                                }
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
        .onChange(of: selectedChapterID) {
            if let chapter = selectedChapter {
                currentPage = chapter.startPage - 1
                covers = chapter.covers
                print("Updated covers: \(covers?.map(\.desc) ?? [])")
            }
        }
        .onChange(of: pdfDocument) { _, newPDFDocument in
            // Move indexing code here
            if let currentPDF = newPDFDocument {
                wordsIndex.indexPDF(from: currentPDF)
                print(wordsIndex.getAllPageTexts())
            }
        }
    }

    var selectedWorkbook: Workbook? {
        workbooks?.first(where: { $0.id == selectedWorkbookID })
    }

    // TODO: Selected chapter should be based on the current page number.
    var selectedChapter: Chapter? {
        chapters?.first(where: { $0.id == selectedChapterID })
    }

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
}

struct SearchBar: View {
    @Binding var text: String

    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)

            TextField("Search chapters and words", text: $text)
                .textFieldStyle(PlainTextFieldStyle())
                .disableAutocorrection(true)

            if !text.isEmpty {
                Button(action: { text = "" },
                       label: {
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

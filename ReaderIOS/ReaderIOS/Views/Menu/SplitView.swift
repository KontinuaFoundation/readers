//
//  SplitView.swift
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

struct SplitView: View {
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
    @State private var searchText = ""
    @State private var wordsIndex = PDFWordsIndex()
    @State private var chapterManager: ChapterManager?
    @State private var searchHighlighter: PDFSearchHighlighter?
    @State private var pdfDocument: PDFDocument?

    var wordSearchResults: [(page: Int, snippet: String)] {
        guard !searchText.isEmpty else { return [] }
        let pageResults = wordsIndex.search(for: searchText)
        return pageResults.sorted { $0.key < $1.key }
            .map { (page: $0.key, snippet: $0.value) }
    }

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            SidebarView(workbooks: $workbooks, selectedWorkbookID: $selectedWorkbookID)
        } content: {
            ContentView(
                isShowingBookmarks: $isShowingBookmarks,
                searchText: $searchText,
                chapters: $chapters,
                currentPage: $currentPage,
                columnVisibility: $columnVisibility,
                wordSearchResults: wordSearchResults,
                bookmarkLookup: $bookmarkLookup,
                currentPdfFileName: $currentPdfFileName
            )
        } detail: {
            DetailView(
                currentPdfFileName: $currentPdfFileName,
                currentPage: $currentPage,
                bookmarkLookup: $bookmarkLookup,
                covers: $covers,
                pdfDocument: $pdfDocument
            )
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
            if let currentPDF = newPDFDocument {
                wordsIndex.indexPDF(from: currentPDF)
                searchHighlighter?.clearHighlights()
                searchHighlighter = PDFSearchHighlighter(pdfDoc: currentPDF)
            }
        }
        .onAppear {
            fetchWorkbooks()
        }
    }

    var selectedWorkbook: Workbook? {
        workbooks?.first(where: { $0.id == selectedWorkbookID })
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
            self.chapterManager = chapterManager
        }
    }

    func updateSelectedChapter() {
        if let chapterManager = chapterManager {
            if let newChapter = chapterManager.getChapter(forPage: currentPage) {
                selectedChapterID = newChapter.id
                covers = newChapter.covers
                print("Updated covers: \(covers?.map(\.desc) ?? [])")
            }
        }
    }
}

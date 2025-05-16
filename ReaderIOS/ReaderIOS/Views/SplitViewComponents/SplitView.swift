//
//  SplitView.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 11/10/24.
//
import PDFKit
import SwiftUI

struct SplitView: View {
    // Loaded workbooks information state vars
    @State var workbooks: [WorkbookPreview]?
    @State var chapters: [Chapter]?
    @State var covers: [Cover]?
    @State var currentCollection: Collection?

    // User selection (what they are viewing) state vars
    @State var selectedWorkbookID: Int?
    @State private var selectedChapterID: String?
    @State var currentWorkbook: Workbook?
    @State private var currentPage: Int = 0
    @State private var columnVisibility = NavigationSplitViewVisibility.automatic

    // Bookmark state vars
    @State private var isShowingBookmarks: Bool = false
    @State private var bookmarkManager = BookmarkManager()

    // PDFDocument loaded by PDFView
    @State private var pdfDocument: PDFDocument?

    // Chapter manager, detemines chapter from current page
    @State private var chapterManager: ChapterManager?

    // Observing networking service for blurring
    @ObservedObject private var networkingSingleton = NetworkingService.shared

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            if let workbooks = workbooks {
                List(workbooks, selection: $selectedWorkbookID) { workbook in
                    HStack {
                        Text("Workbook \(workbook.number)")
                            .tag(workbook.id)
                    }
                }
            } else {
                ProgressView("Fetching workbooks...")
            }
        }
        content: {
            Group {
                if !isShowingBookmarks {
                    // Search view
                    SearchView(
                        currentPage: $currentPage,
                        columnVisibility: $columnVisibility,
                        chapters: chapters,
                        fetchWorkbookAndChapters: fetchWorkbookAndChapters,
                        pdfDocument: pdfDocument
                    )
                } else {
                    // Bookmarks view
                    BookmarkSearchView(
                        currentPage: $currentPage,
                        currentWorkbook: currentWorkbook,
                        bookmarkManager: bookmarkManager
                    )
                    .blur(radius: networkingSingleton.isContentLoading ? 10 : 0)
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
            // TODO: Only give access to bookmarks for current file.
            PDFView(
                currentWorkbook: $currentWorkbook,
                currentPage: $currentPage,
                covers: $covers,
                pdfDocument: $pdfDocument,
                collection: $currentCollection,
                bookmarkManager: bookmarkManager
            )
            .blur(radius: networkingSingleton.isContentLoading ? 10 : 0)
        }
        .onAppear {
            if let selectedWorkbookID {
                currentPage = StateRestoreManager.shared.loadPageNumber(for: selectedWorkbookID)
            }
        }
        .onChange(of: selectedWorkbookID) {
            fetchWorkbookAndChapters()

            if let selectedWorkbookID {
                currentPage = StateRestoreManager.shared.loadPageNumber(for: selectedWorkbookID)
            }
        }
        .onChange(of: currentPage) { _, _ in
            updateSelectedChapter()

            persistState()
        }
    }

    var selectedWorkbook: WorkbookPreview? {
        workbooks?.first(where: { $0.id == selectedWorkbookID })
    }

    func persistState() {
        if let selectedWorkbookID {
            StateRestoreManager.shared.saveState(workbookID: selectedWorkbookID, pageNumber: currentPage)
        }
    }

    func fetchWorkbookAndChapters() {
        guard let id = selectedWorkbook?.id else { return }

        NetworkingService.shared.fetchWorkbook(id: id) { result in
            switch result {
            case let .success(workbookRes):
                chapters = workbookRes.chapters
                currentWorkbook = workbookRes
                setupChapterManager()
            case let .failure(error):
                print("Error fetching chapters: \(error)")
            }
        }
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
            if let newChapter = chapterManager.getChapter(forPage: currentPage) {
                selectedChapterID = newChapter.id
                covers = newChapter.covers
                print("Updated covers: \(covers?.map(\.desc) ?? [])")
            }
        }
    }
}

/*
 #Preview {
 SplitView()
 }
 */

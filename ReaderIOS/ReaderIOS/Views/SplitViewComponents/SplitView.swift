//
//  SplitView.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 11/10/24.
//
import PDFKit
import SwiftUI

struct SplitView: View {
    var initialWorkbooks: [WorkbookPreview] = []
    var initialWorkbookID: Int?
    var initialPDFDocument: PDFDocument?
    var initialCollection: Collection?

    // Loaded workbooks information state vars
    @State private var workbooks: [WorkbookPreview]?
    @State private var chapters: [Chapter]?
    @State private var covers: [Cover]?

    // User selection (what they are viewing) state vars
    @State private var selectedWorkbookID: Int?
    @State private var selectedChapterID: String?
    @State private var currentWorkbook: Workbook?
    @State private var currentPage: Int = 0
    @State private var columnVisibility = NavigationSplitViewVisibility.automatic

    // Bookmark state vars
    @State private var isShowingBookmarks: Bool = false
    @State private var bookmarkManager = BookmarkManager()

    // PDFDocument loaded by PDFView
    @State private var pdfDocument: PDFDocument?

    // Chapter manager, detemines chapter from current page
    @State private var chapterManager: ChapterManager?

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
                ProgressView("Fetching Workbooks")
                    .onAppear {
                        if !initialWorkbooks.isEmpty {
                            workbooks = initialWorkbooks
                            selectedWorkbookID = initialWorkbookID
                        }
                    }
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
            if currentWorkbook != nil {
                // TODO: Only give access to bookmarks for current file.
                PDFView(
                    currentWorkbook: $currentWorkbook,
                    currentPage: $currentPage,
                    covers: $covers,
                    pdfDocument: $pdfDocument,
                    bookmarkManager: bookmarkManager
                )
            } else {
                ProgressView("Getting the latest workbook.")
            }
        }
        .onAppear {
            // Optionally, if initialPDFDocument is available, set it.
            if let initialPDF = initialPDFDocument {
                pdfDocument = initialPDF
            }
        }
        .onChange(of: selectedWorkbookID) {
            guard let selectedWorkbook = selectedWorkbook else { return }

            fetchWorkbookAndChapters()

            if let selectedWorkbookID {
                currentPage = StateRestoreManager.shared.loadPageNumber(for: selectedWorkbookID)
            }

            persistState()
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

    func fetchWorkbooks() {
        guard let initialCollection else { return }

        NetworkingService.shared.fetchWorkbooks(collection: initialCollection) { result in
            switch result {
            case let .success(workbookResponse):
                workbooks = workbookResponse

                // Try to load saved state now that workbooks are available.
                if let savedState = StateRestoreManager.shared.loadState() {
                    if workbookResponse.first(where: { $0.id == savedState.workbookID }) != nil {
                        selectedWorkbookID = savedState.workbookID
                        currentPage = savedState.pageNumber
                    } else {
                        // Fallback: default to the first workbook if the saved one isn't found.
                        print("defaulting to first ")
                        selectedWorkbookID = workbookResponse.first?.id
                    }
                } else {
                    // No saved state, so select the first workbook by default.
                    print("no saved state")
                    selectedWorkbookID = workbookResponse.first?.id
                }
            case let .failure(error):
                print("Error fetching workbooks: \(error)")
            }
        }
    }

    func fetchWorkbookAndChapters() {
        guard let id = selectedWorkbook?.id else { return }

        NetworkingService.shared.fetchWorkbook(id: id) { result in
            switch result {
            case let .success(workbookRes):
                chapters = workbookRes.chapters
                selectedChapterID = chapters?.first?.id
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

#Preview {
    SplitView()
}

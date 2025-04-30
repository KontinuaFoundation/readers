//
//  SplitView.swift
//  ReaderIOS
//
//  Created by Devin Hadley on 11/10/24.
//
import PDFKit
import SwiftUI

struct SplitView: View {
    /*
    var initialWorkbooks: [WorkbookPreview] = []
    var initialPDFDocument: PDFDocument?
    var initialCollection: Collection?
     */
    
    //var initialWorkbookID: Int?
    
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
                /*
                ProgressView("Fetching Workbooks")
                    .onAppear {
                        
                        
                    }
                 */
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
            if currentWorkbook != nil {
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
            } else {
                ProgressView("Getting the latest workbook.")
            }
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

    /*
    func fetchWorkbooks() {
        NetworkingService.shared.fetchWorkbooks(collection: currentCollection) { result in
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
    }*/

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

import PDFKit
import SwiftUI

enum AnnotationMode {
    case pen
    case highlight
    case erase
    case text
    case none
}

struct PDFView: View {
    // MARK: - Bindings

    @Binding var currentWorkbook: Workbook?
    @Binding var currentPage: Int
    @Binding var covers: [Cover]?
    @Binding var pdfDocument: PDFDocument?
    @Binding var collection: Collection?

    // MARK: - Observed Objects

    @ObservedObject var bookmarkManager: BookmarkManager

    // MARK: - State Variables

    // misc variables
    @State private var showDigitalResources = false
    @State private var showingFeedback = false
    // markup variables
    @State var mode: AnnotationMode = .none
    @State private var exitNotSelected = false
    @State private var pagePaths: [String: [(path: Path, color: Color)]] = [:]
    @State private var highlightPaths: [String: [(path: Path, color: Color)]] = [:]
    @State private var selectedPenColor: Color = .clear
    @State private var selectedHighlighterColor: Color = .clear
    @State private var isPenSubmenuVisible = false
    @State private var textBoxes: [String: [TextBoxData]] = [:]
    @State private var deleteTextBox = false
    @State private var currentTextBox = -1
    @State private var textOpened = false
    @State private var isHidden = false
    @State private var showClearAlert = false
    @State private var pdfPageFrame: CGRect = .zero

    // MARK: - StateObjects and Observed

    @StateObject private var feedbackManager = FeedbackManager()
    @StateObject private var timerManager = TimerManager()
    @ObservedObject private var zoomManager = ZoomManager()
    @ObservedObject private var textManager = TextManager()
    @ObservedObject private var annotationStorageManager = AnnotationStorageManager()

    // MARK: - Body

    var body: some View {
        GeometryReader { geometry in
            NavigationStack {
                ZStack {
                    if let pdfDoc = pdfDocument {
                        ZStack {
                            DocumentView(
                                pdfDocument: pdfDoc,
                                currentPageIndex: $currentPage,
                                pageFrame: $pdfPageFrame
                            )
                            .edgesIgnoringSafeArea(.all)
                            .scaleEffect(zoomManager.newZoomLevel(),
                                         anchor: zoomManager.getZoomedIn() ? zoomManager.getZoomPoint() : .center)
                            .onChange(of: currentPage) { _, newValue in
                                loadPaths(for: newValue)
                                if let workbookId = currentWorkbook?.id {
                                    Logger.PDF.pageChanged(to: newValue, workbook: workbookId)
                                }
                            }

                            // Annotations
                            AnnotationsView(
                                pagePaths: $pagePaths,
                                highlightPaths: $highlightPaths,
                                selectedScribbleTool: $mode,
                                textOpened: $textOpened,
                                key: uniqueKey(for: currentPage),
                                nextPage: { goToNextPage() },
                                previousPage: { goToPreviousPage() },
                                selectedColor: $selectedPenColor,
                                selectedHighlighterColor: $selectedHighlighterColor,
                                zoomedIn: zoomManager.getZoomedIn(),
                                zoomManager: zoomManager,
                                annotationManager: annotationStorageManager,
                                textManager: textManager,
                                textBoxes: $textBoxes,
                                isHidden: $isHidden,
                                pageFrame: pdfPageFrame
                            )
                            .scaleEffect(zoomManager.newZoomLevel(),
                                         anchor: zoomManager.getZoomedIn() ? zoomManager.getZoomPoint() : .center)
                            // Annotations Text Overlay
                            TextView(
                                textManager: textManager,
                                textBoxes: $textBoxes,
                                key: uniqueKey(for: currentPage),
                                deleteTextBox: $deleteTextBox,
                                currentTextBoxIndex: $currentTextBox,
                                width: geometry.size.width,
                                height: geometry.size.height,
                                textOpened: $textOpened,
                                isHidden: $isHidden,
                                pageFrame: pdfPageFrame
                            )
                            .scaleEffect(zoomManager.newZoomLevel(),
                                         anchor: zoomManager.getZoomedIn() ? zoomManager.getZoomPoint() : .center)
                            .alert("Are you sure you want to delete the text box?",
                                   isPresented: $deleteTextBox)
                            {
                                Button("Delete", role: .destructive) {
                                    textOpened = false
                                    textManager.deleteText(
                                        textBoxes: $textBoxes,
                                        key: uniqueKey(for: currentPage),
                                        index: currentTextBox
                                    )
                                    textManager.saveTextBoxes(textBoxes: textBoxes)
                                    currentTextBox = -1
                                }
                                Button("Cancel", role: .cancel) {
                                    currentTextBox = -1
                                }
                            }
                        }
                        .ignoresSafeArea(.keyboard)
                        .toolbar {
                            ToolbarItemGroup(placement: .navigationBarLeading) {
                                PageControlView(currentPage: $currentPage, totalPages: pdfDoc.pageCount)
                                    .padding(.leading, 10)
                            }

                            ToolbarItemGroup(placement: .navigationBarTrailing) {
                                TimerControlsView(timerManager: timerManager)
                                MarkupMenu(
                                    selectedScribbleTool: $mode,
                                    exitNotSelected: $exitNotSelected,
                                    showClearAlert: $showClearAlert,
                                    selectedPenColor: $selectedPenColor,
                                    selectedHighlighterColor: $selectedHighlighterColor,
                                    isPenSubmenuVisible: $isPenSubmenuVisible,
                                    textBoxes: $textBoxes,
                                    annotationManager: annotationStorageManager,
                                    textManager: textManager,
                                    pagePaths: pagePaths,
                                    highlightPaths: highlightPaths
                                )

                                Button(action: { showDigitalResources = true }, label: {
                                    Text("Digital Resources")
                                        .padding(5)
                                        .foregroundColor((covers?.isEmpty ?? true) ? .gray : .purple)
                                        .cornerRadius(8)
                                })
                                .disabled(covers?.isEmpty ?? true)
                                .fullScreenCover(isPresented: $showDigitalResources, content: {
                                    DigitalResourcesView(covers: covers)
                                })

                                Button {
                                    bookmarkManager.toggleBookmark(for: currentWorkbook, currentPage: currentPage)
                                } label: {
                                    Image(systemName: bookmarkManager.isBookmarked(
                                        workbook: currentWorkbook,
                                        currentPage: currentPage
                                    ) ? "bookmark.fill" : "bookmark")
                                        .foregroundColor(.yellow)
                                }

                                if zoomManager.getZoomedIn() {
                                    Button("Reset Zoom") {
                                        zoomManager.resetZoom()
                                    }
                                }
                            }
                            ToolbarItemGroup(placement: .bottomBar) {
                                VStack(spacing: 0) {
                                    Spacer(minLength: 20)
                                    HStack(spacing: 0) {
                                        TimerProgressView(timerManager: timerManager)
                                            .frame(maxWidth: .infinity)
                                        FeedbackButton(
                                            feedbackManager: feedbackManager,
                                            workbook: currentWorkbook,
                                            currentPage: currentPage,
                                            collection: collection
                                        )
                                        .padding(.leading, 5)
                                    }
                                }
                            }
                        }
                    } else {
                        ProgressView("Getting workbook...")
                            .onAppear {
                                loadPDFDocument()
                                annotationStorageManager.loadAnnotations(pagePaths: &pagePaths,
                                                                         highlightPaths: &highlightPaths)
                                textManager.loadTextBoxes(textBoxes: &textBoxes)
                            }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .onDisappear {
                    textManager.saveTextBoxes(textBoxes: textBoxes)
                    annotationStorageManager.saveAnnotations(pagePaths: pagePaths, highlightPaths: highlightPaths)
                }
            }
            .alert("Are you sure you want to clear your screen?", isPresented: $showClearAlert) {
                Button("Clear", role: .destructive) {
                    clearMarkup()
                    annotationStorageManager.saveAnnotations(pagePaths: pagePaths, highlightPaths: highlightPaths)
                }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(isPresented: $feedbackManager.isShowingFeedback) {
                FeedbackView(feedbackManager: feedbackManager)
            }
            .onChange(of: currentWorkbook) { _, _ in
                loadPDFDocument()
            }
        }
    }

    // MARK: - Helper Methods

    private func loadPDFDocument() {
        guard let currentWorkbook = currentWorkbook else {
            Logger.warning("No workbook available to load PDF", category: "PDF")
            return
        }

        Logger.PDF.documentLoaded(currentWorkbook.id)

        NetworkingService.shared.fetchPDF(workbook: currentWorkbook) { result in
            switch result {
            case let .success(document):
                pdfDocument = document
                Logger.info("PDF loaded successfully for workbook: \(currentWorkbook.id)", category: "PDF")
            case let .failure(error):
                Logger.error(
                    "Error fetching PDF for workbook \(currentWorkbook.id): \(error.localizedDescription)",
                    category: "PDF"
                )
            }
        }
    }

    private func loadPaths(for pageIndex: Int) {
        let key = uniqueKey(for: pageIndex)
        if pagePaths[key] == nil { pagePaths[key] = [] }
        if highlightPaths[key] == nil { highlightPaths[key] = [] }
    }

    private func uniqueKey(for pageIndex: Int) -> String {
        guard let workbook = currentWorkbook else { return "\(pageIndex)" }
        return "\(workbook.id)-\(pageIndex)"
    }

    private func clearMarkup() {
        let key = uniqueKey(for: currentPage)
        highlightPaths.removeValue(forKey: key)
        pagePaths.removeValue(forKey: key)
        textManager.deleteAllText(textBoxes: $textBoxes, key: uniqueKey(for: currentPage))
    }

    private func goToNextPage() {
        if let pdfDoc = pdfDocument, currentPage < pdfDoc.pageCount - 1 {
            currentPage += 1
        }
    }

    private func goToPreviousPage() {
        if currentPage > 0 {
            currentPage -= 1
        }
    }
}

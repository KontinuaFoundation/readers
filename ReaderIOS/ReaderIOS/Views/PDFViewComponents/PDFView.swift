import PDFKit
import SwiftUI

struct URLItem: Identifiable {
    let id = UUID()
    let url: URL
}

struct PDFView: View {
    @Binding var fileName: String?
    @Binding var currentPage: Int
    // Remove the bookmarkLookup binding:
    // @Binding var bookmarkLookup: [String: Set<Int>]
    @Binding var covers: [Cover]?
    @Binding var pdfDocument: PDFDocument?

    // Digital resources state vars
    @State private var showDigitalResources = false
    @ObservedObject private var zoomManager = ZoomManager()
    @State private var showingFeedback = false

    // Timer vars
    @StateObject private var timerManager = TimerManager()

    // Annotation vars
    @State private var exitNotSelected: Bool = false
    @State private var selectedScribbleTool: String = ""
    @State private var pageChangeEnabled: Bool = true
    @State private var pagePaths: [String: [(path: Path, color: Color)]] = [:]
    @State private var highlightPaths: [String: [(path: Path, color: Color)]] = [:]
    @State private var selectedPenColor: Color = .black // Default pen color is black
    @State private var selectedHighlighterColor: Color = .yellow
    @State private var isPenSubmenuVisible: Bool = false
    @State var textBoxes: [String: [TextBoxData]] = [:]
    @ObservedObject private var textManager = TextManager()
    @State var deleteTextBox: Bool = false
    @State var currentTextBox: Int = -1
    @State var textOpened: Bool = false
    @State var isHidden: Bool = false

    @State private var showClearAlert = false
    @ObservedObject private var annotationStorageManager = AnnotationStorageManager()

    // Use the new BookmarkManager instead of a binding dictionary.
    @ObservedObject var bookmarkManager: BookmarkManager

    var body: some View {
        GeometryReader { geometry in
            NavigationStack {
                ZStack {
                    VStack {
                        if let pdfDocument = pdfDocument {
                            ZStack {
                                DocumentView(
                                    pdfDocument: pdfDocument,
                                    currentPageIndex: $currentPage
                                )
                                .edgesIgnoringSafeArea(.all)
                                .scaleEffect(
                                    zoomManager.newZoomLevel(),
                                    anchor: zoomManager.getZoomedIn() ? zoomManager.getZoomPoint() : .center
                                )
                                .onChange(of: currentPage) { _, newValue in
                                    loadPathsForPage(newValue)
                                }
                                AnnotationsView(
                                    pagePaths: $pagePaths,
                                    highlightPaths: $highlightPaths,
                                    key: uniqueKey(for: currentPage),
                                    selectedScribbleTool: $selectedScribbleTool,
                                    nextPage: { goToNextPage() },
                                    previousPage: { goToPreviousPage() },
                                    annotationManager: annotationStorageManager,
                                    textManager: textManager,
                                    textBoxes: $textBoxes,
                                    selectedColor: selectedPenColor,
                                    selectedHighlighterColor: selectedHighlighterColor,
                                    zoomedIn: zoomManager.getZoomedIn(),
                                    textOpened: $textOpened
                                )
                                .scaleEffect(
                                    zoomManager.newZoomLevel(),
                                    anchor: zoomManager.getZoomedIn() ? zoomManager.getZoomPoint() : .center
                                )
                                .gesture(zoomManager.zoomin())
                                .gesture(zoomManager.zoomout())
                                .onTapGesture(count: 1, coordinateSpace: .local) { location in
                                    zoomManager.newZoomPoint(
                                        newPoint: location,
                                        width: geometry.size.width,
                                        height: geometry.size.height
                                    )
                                    if !textOpened, selectedScribbleTool == "Text" {
                                        textOpened = true
                                        textManager.addText(
                                            textBoxes: $textBoxes,
                                            key: uniqueKey(for: currentPage),
                                            width: geometry.size.width,
                                            height: geometry.size.height
                                        )
                                        textManager.saveTextBoxes(textBoxes: textBoxes)
                                    } else {
                                        textOpened = false
                                    }
                                }
                                TextView(
                                    textManager: textManager,
                                    textBoxes: $textBoxes,
                                    key: uniqueKey(for: currentPage),
                                    deleteTextBox: $deleteTextBox,
                                    currentTextBoxIndex: $currentTextBox,
                                    width: geometry.size.width,
                                    height: geometry.size.height,
                                    textOpened: $textOpened,
                                    isHidden: $isHidden
                                )
                                .scaleEffect(
                                    zoomManager.newZoomLevel(),
                                    anchor: zoomManager.getZoomedIn() ? zoomManager.getZoomPoint() : .center
                                )
                                .alert(
                                    "Are you sure you want to delete the text box?",
                                    isPresented: $deleteTextBox
                                ) {
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
                            .toolbar {
                                ToolbarItemGroup(placement: .navigationBarLeading) {
                                    PageControlView(currentPage: $currentPage, totalPages: pdfDocument.pageCount)
                                        .padding(.leading, 10)
                                }

                                ToolbarItemGroup(placement: .navigationBarTrailing) {
                                    TimerControlsView(timerManager: timerManager)
                                    MarkupMenu(
                                        selectedScribbleTool: $selectedScribbleTool,
                                        exitNotSelected: $exitNotSelected,
                                        showClearAlert: $showClearAlert,
                                        selectedPenColor: $selectedPenColor,
                                        selectedHighlighterColor: $selectedHighlighterColor,
                                        isPenSubmenuVisible: $isPenSubmenuVisible,
                                        annotationManager: annotationStorageManager,
                                        textManager: textManager,
                                        textBoxes: $textBoxes,
                                        pagePaths: pagePaths,
                                        highlightPaths: highlightPaths
                                    )
                                    Button(action: { showDigitalResources = true },
                                           label: {
                                               Text("Digital Resources")
                                                   .padding(5)
                                                   .foregroundColor((covers?.isEmpty ?? true) ? .gray : .purple)
                                                   .cornerRadius(8)
                                           })
                                           .disabled(covers?.isEmpty ?? true)
                                           .fullScreenCover(isPresented: $showDigitalResources) {
                                               DigitalResourcesView(covers: covers)
                                           }

                                    // Bookmark button using the BookmarkManager
                                    Button {
                                        bookmarkManager.toggleBookmark(for: fileName, currentPage: currentPage)
                                    } label: {
                                        Image(systemName: bookmarkManager.isBookmarked(
                                            fileName: fileName,
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

                                ToolbarItem(placement: .bottomBar) {
                                    TimerProgressView(timerManager: timerManager, showingFeedback: $showingFeedback)
                                }
                            }
                        } else {
                            ProgressView("Getting Workbook")
                                .onAppear {
                                    loadPDFFromURL()
                                    annotationStorageManager.loadAnnotations(
                                        pagePaths: &pagePaths,
                                        highlightPaths: &highlightPaths
                                    )
                                    textManager.loadTextBoxes(textBoxes: &textBoxes)
                                }
                        }
                    }
                }
                .onDisappear {
                    textManager.saveTextBoxes(textBoxes: textBoxes)
                    annotationStorageManager.saveAnnotations(pagePaths: pagePaths, highlightPaths: highlightPaths)
                }
            }
            .alert("Are you sure you want to clear your screen?", isPresented: $showClearAlert) {
                Button("Clear", role: .destructive) {
                    clearMarkup()
                    annotationStorageManager.saveAnnotations(
                        pagePaths: pagePaths,
                        highlightPaths: highlightPaths
                    )
                }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(isPresented: $showingFeedback) {
                FeedbackView()
            }
            .onChange(of: fileName) { _, _ in
                loadPDFFromURL()
            }
        }
    }

    // MARK: - Navigation Helpers

    private func goToNextPage() {
        if let pdfDocument = pdfDocument, currentPage < pdfDocument.pageCount - 1 {
            currentPage += 1
        }
    }

    private func goToPreviousPage() {
        if currentPage > 0 {
            currentPage -= 1
        }
    }

    // MARK: - PDF Loading and Path Management

    private func loadPDFFromURL() {
        guard let fileName = fileName else { return }
            NetworkingService.shared.fetchPDF(fileName: fileName) { result in
                switch result {
                case .success(let document):
                    DispatchQueue.main.async {
                        pdfDocument = document
                    }
                case .failure(let error):
                    print("Error fetching PDF: \(error.localizedDescription)")
                }
            }
    }

    private func loadPathsForPage(_ pageIndex: Int) {
        let key = uniqueKey(for: pageIndex)
        if pagePaths[key] == nil {
            pagePaths[key] = []
        }
        if highlightPaths[key] == nil {
            highlightPaths[key] = []
        }
    }

    private func uniqueKey(for pageIndex: Int) -> String {
        guard let fileName = fileName else { return "\(pageIndex)" }
        return "\(fileName)-\(pageIndex)"
    }

    private func clearMarkup() {
        highlightPaths.removeValue(forKey: uniqueKey(for: currentPage))
        pagePaths.removeValue(forKey: uniqueKey(for: currentPage))
    }
}

#Preview {
    SplitView()
}

import PDFKit
import SwiftUI

struct URLItem: Identifiable {
    let id = UUID()
    let url: URL
}

struct PDFView: View {
    @Binding var fileName: String?
    @Binding var currentPage: Int
    @Binding var bookmarkLookup: [String: Set<Int>]
    @Binding var covers: [Cover]?
    @Binding var pdfDocument: PDFDocument?

    // Digital resources state vars
    @State private var showDigitalResources = false

    // zoom vars
    @ObservedObject private var zoomManager = ZoomManager()

    // feedback var
    @State private var showingFeedback = false

    // timer vars
    @StateObject private var timerManager = TimerManager()

    // annotation vars
    @State private var annotationsEnabled: Bool = false
    @State private var exitNotSelected: Bool = false
    @State private var selectedScribbleTool: String = ""
    @State private var pageChangeEnabled: Bool = true
    @State private var pagePaths: [String: [(path: Path, color: Color)]] = [:]
    @State private var highlightPaths: [String: [(path: Path, color: Color)]] = [:]
    @State private var selectedPenColor: Color = .black // pen default is black
    @State private var selectedHighlighterColor: Color = .yellow // highlight default si yellow

    @State private var isPenSubmenuVisible: Bool = false

    @State private var showClearAlert = false
    @ObservedObject private var annotationManager = AnnotationManager()

    // big pdf view
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
                                    anchor: zoomManager.zoomedIn ? zoomManager.zoomPoint : .center
                                )
                                .onChange(of: currentPage) { _, newValue in
                                    loadPathsForPage(newValue)
                                }
                                if annotationsEnabled {
                                    AnnotationsView(
                                        pagePaths: $pagePaths,
                                        highlightPaths: $highlightPaths,
                                        key: uniqueKey(for: currentPage),
                                        selectedScribbleTool: $selectedScribbleTool,
                                        nextPage: { goToNextPage() },
                                        previousPage: { goToPreviousPage() },
                                        annotationManager: annotationManager,
                                        selectedColor: selectedPenColor,
                                        selectedHighlighterColor: selectedHighlighterColor,
                                        zoomedIn: zoomManager.zoomedIn
                                    )
                                    .scaleEffect(
                                        zoomManager.newZoomLevel(),
                                        anchor: zoomManager.zoomedIn ? zoomManager.zoomPoint : .center
                                    )
                                    .gesture(zoomManager.zoomin())
                                    .gesture(zoomManager.zoomout())
                                    .onTapGesture(count: 1, coordinateSpace: .local) { location in
                                        zoomManager.newZoomPoint(
                                            newPoint: location,
                                            width: geometry.size.width,
                                            height: geometry.size.height
                                        )
                                    }
                                }
                            }
                            .toolbar {
                                ToolbarItemGroup(placement: .navigationBarLeading) {
                                    PageControlView(currentPage: $currentPage, totalPages: pdfDocument.pageCount)
                                        .padding(.leading, 10)
                                }

                                ToolbarItemGroup(placement: .navigationBarTrailing) {
                                    // timer controls now in to TimerControlsView
                                    TimerControlsView(timerManager: timerManager)
                                    // markup menu now in MarkupView
                                    MarkupMenu(
                                        selectedScribbleTool: $selectedScribbleTool,
                                        annotationsEnabled: $annotationsEnabled,
                                        exitNotSelected: $exitNotSelected,
                                        showClearAlert: $showClearAlert,
                                        selectedPenColor: $selectedPenColor,
                                        selectedHighlighterColor: $selectedHighlighterColor,
                                        isPenSubmenuVisible: $isPenSubmenuVisible,
                                        annotationManager: annotationManager,
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

                                    Button {
                                        toggleCurrentPageInBookmarks()
                                    } label: {
                                        Image(systemName: isCurrentPageBookmarked ? "bookmark.fill" : "bookmark")
                                            .foregroundColor(.yellow)
                                    }

                                    if zoomManager.zoomedIn {
                                        Button("Reset Zoom") {
                                            zoomManager.resetZoom()
                                        }
                                    }
                                }

                                // bottom bar now using TimerProgressView
                                ToolbarItem(placement: .bottomBar) {
                                    TimerProgressView(timerManager: timerManager, showingFeedback: $showingFeedback)
                                }
                            }
                        } else {
                            ProgressView("Getting Workbook")
                                .onAppear {
                                    loadPDFFromURL()
                                    annotationManager.loadAnnotations(
                                        pagePaths: &pagePaths,
                                        highlightPaths: &highlightPaths
                                    )
                                    if !pagePaths.isEmpty || !highlightPaths.isEmpty {
                                        annotationsEnabled = true
                                    }
                                }
                        }
                    }
                }
            }
            .alert("Are you sure you want to clear your screen?", isPresented: $showClearAlert) {
                Button("Clear", role: .destructive) {
                    clearMarkup()
                    annotationManager.saveAnnotations(
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

    private func dragGesture() -> some Gesture {
        if pageChangeEnabled {
            return DragGesture().onEnded { value in
                if value.translation.width < 0 {
                    goToNextPage()
                } else if value.translation.width > 0 {
                    goToPreviousPage()
                }
            }
        } else {
            return DragGesture().onEnded { _ in }
        }
    }

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

    private func loadPDFFromURL() {
        guard let fileName = fileName else { return }

        let baseURL = "http://localhost:8000/pdfs/"
        let urlString = baseURL + fileName
        guard let url = URL(string: urlString) else {
            print("Invalid URL for file: \(fileName)")
            return
        }

        URLSession.shared.dataTask(with: url) { data, _, error in
            if let error = error {
                print("Error downloading PDF: \(error.localizedDescription)")
                return
            }
            guard let data = data, let document = PDFDocument(data: data) else {
                print("No data found or invalid PDF from \(url).")
                return
            }
            DispatchQueue.main.async {
                pdfDocument = document
            }
        }.resume()
    }

    private func selectScribbleTool(_ tool: String) {
        selectedScribbleTool = tool
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

    var isCurrentPageBookmarked: Bool {
        if let fileName = fileName {
            if let valueSet = bookmarkLookup[fileName] {
                return valueSet.contains(currentPage)
            }
            return false
        }
        return false
    }

    private func toggleCurrentPageInBookmarks() {
        if let fileName = fileName {
            if var valueSet = bookmarkLookup[fileName] {
                if valueSet.contains(currentPage) {
                    valueSet.remove(currentPage)
                } else {
                    valueSet.insert(currentPage)
                }
                bookmarkLookup[fileName] = valueSet
            } else {
                bookmarkLookup[fileName] = Set([currentPage])
            }
        }
    }
}

#Preview {
    SplitView()
}

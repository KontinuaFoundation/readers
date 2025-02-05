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
    @ObservedObject private var timerManager = TimerManager()

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
                                ToolbarItemGroup(placement: .navigationBarTrailing) {
                                    PageControlView(currentPage: $currentPage, totalPages: pdfDocument.pageCount)
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

// MARK: - Markup Menu View

struct MarkupMenu: View {
    @Binding var selectedScribbleTool: String
    @Binding var annotationsEnabled: Bool
    @Binding var exitNotSelected: Bool
    @Binding var showClearAlert: Bool
    @Binding var selectedPenColor: Color
    @Binding var selectedHighlighterColor: Color
    @Binding var isPenSubmenuVisible: Bool
    @ObservedObject var annotationManager: AnnotationManager

    var pagePaths: [String: [(path: Path, color: Color)]]
    var highlightPaths: [String: [(path: Path, color: Color)]]

    var body: some View {
        Menu {
            // pen submenu
            Menu {
                Button {
                    selectedPenColor = .black
                    selectScribbleTool("Pen")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Black")
                    if selectedPenColor == .black {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedPenColor = .green
                    selectScribbleTool("Pen")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Green")
                    if selectedPenColor == .green {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedPenColor = .red
                    selectScribbleTool("Pen")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Red")
                    if selectedPenColor == .red {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedPenColor = .blue
                    selectScribbleTool("Pen")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Blue")
                    if selectedPenColor == .blue {
                        Image(systemName: "checkmark")
                    }
                }
            } label: {
                HStack {
                    Text("Pen")
                    if selectedScribbleTool == "Pen" {
                        Circle()
                            .fill(selectedPenColor)
                            .frame(width: 10, height: 10)
                    }
                }
            }
            Menu {
                Button {
                    selectedHighlighterColor = .yellow
                    selectScribbleTool("Highlight")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Yellow")
                    if selectedHighlighterColor == .yellow {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedHighlighterColor = .pink
                    selectScribbleTool("Highlight")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Pink")
                    if selectedHighlighterColor == .pink {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedHighlighterColor = .blue
                    selectScribbleTool("Highlight")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Blue")
                    if selectedHighlighterColor == .blue {
                        Image(systemName: "checkmark")
                    }
                }

                Button {
                    selectedHighlighterColor = .green
                    selectScribbleTool("Highlight")
                    annotationsEnabled = true
                    exitNotSelected = true
                    isPenSubmenuVisible = false
                } label: {
                    Text("Green")
                    if selectedHighlighterColor == .green {
                        Image(systemName: "checkmark")
                    }
                }
            } label: {
                HStack {
                    Text("Highlight")
                    if selectedScribbleTool == "Highlight" {
                        Circle()
                            .fill(selectedHighlighterColor)
                            .frame(width: 10, height: 10)
                    }
                }
            }
            Button("Erase") {
                selectScribbleTool("Erase")
                annotationsEnabled = true
                exitNotSelected = true
            }
            Button("Text") {
                selectScribbleTool("Text")
                annotationsEnabled = true
                exitNotSelected = true
            }
            Button("Clear Screen") {
                showClearAlert = true
            }
            Button("Exit Markup") {
                selectScribbleTool("")
                exitNotSelected = false
                annotationManager.saveAnnotations(
                    pagePaths: pagePaths,
                    highlightPaths: highlightPaths
                )
            }
        } label: {
            Text(selectedScribbleTool.isEmpty ? "Markup" : "Markup: " + selectedScribbleTool)
                .padding(5)
                .foregroundColor(exitNotSelected ? Color.pink : Color.blue)
                .cornerRadius(8)
        }
    }

    private func selectScribbleTool(_ tool: String) {
        selectedScribbleTool = tool
    }
}

// MARK: - Timer Controls View

struct TimerControlsView: View {
    @ObservedObject var timerManager: TimerManager

    var body: some View {
        // Timer Controls
        if timerManager.isTimerRunning {
            Button {
                timerManager.pauseTimer()
            } label: {
                Image(systemName: "pause.circle")
                    .foregroundColor(.yellow)
            }

            Button {
                timerManager.restartTimer()
            } label: {
                Image(systemName: "arrow.clockwise.circle")
                    .foregroundColor(.blue)
            }

            Button {
                timerManager.cancelTimer()
            } label: {
                Image(systemName: "xmark.circle")
                    .foregroundColor(.red)
            }
        } else if timerManager.isPaused {
            Button {
                timerManager.unpauseTimer()
            } label: {
                Image(systemName: "play.circle")
                    .foregroundColor(.green)
            }

            Button {
                timerManager.restartTimer()
            } label: {
                Image(systemName: "arrow.clockwise.circle")
                    .foregroundColor(.blue)
            }

            Button {
                timerManager.cancelTimer()
            } label: {
                Image(systemName: "xmark.circle")
                    .foregroundColor(.red)
            }
        } else {
            Menu {
                Button("15 Minutes") {
                    timerManager.startTimer(duration: 15 * 60)
                }
                Button("20 Minutes") {
                    timerManager.startTimer(duration: 20 * 60)
                }
                Button("25 Minutes") {
                    timerManager.startTimer(duration: 25 * 60)
                }
                Button("Clear Timer") {
                    timerManager.cancelTimer()
                }
            } label: {
                Text("Timer")
                    .padding(5)
                    .foregroundColor(.blue)
                    .cornerRadius(8)
            }
        }
    }
}

// MARK: - Timer Progress View

struct TimerProgressView: View {
    @ObservedObject var timerManager: TimerManager
    @Binding var showingFeedback: Bool

    var body: some View {
        HStack(spacing: 0) {
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    if timerManager.isTimerRunning || timerManager.isPaused {
                        Rectangle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(width: geometry.size.width, height: 4)
                    }

                    Rectangle()
                        .fill(timerManager.isPaused ? Color
                            .yellow : (timerManager.progress >= 1 ? Color.green : Color.red))
                        .frame(
                            width: geometry.size.width * CGFloat(timerManager.progress),
                            height: 4
                        )
                        .animation(.linear(duration: 0.1), value: timerManager.progress)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: 4)

            Button {
                showingFeedback = true
            } label: {
                Image(systemName: "message.fill")
                    .font(.system(size: 16))
                    .foregroundColor(.white)
                    .padding(8)
                    .background(Color.blue)
                    .clipShape(Circle())
                    .shadow(radius: 2)
            }
        }
        .padding(.leading, 25)
    }
}

#Preview {
    NavigationPDFSplitView()
}

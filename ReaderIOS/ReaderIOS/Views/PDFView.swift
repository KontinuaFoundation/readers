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

    @State private var resetZoom = false
    @State private var zoomedIn = false
    @State private var showingFeedback = false

    @ObservedObject private var timerManager = TimerManager()
    @State private var annotationsEnabled: Bool = false
    @State private var exitNotSelected: Bool = false
    @State private var selectedScribbleTool: String = ""
    @State private var pageChangeEnabled: Bool = true
    @State private var pagePaths: [String: [Path]] = [:]
    @State private var highlightPaths: [String: [Path]] = [:]
    @State private var textAnnotations: [String: [TextAnnotation]] = [:]

    @State private var showClearAlert = false
    @ObservedObject private var annotationManager = AnnotationManager()

    var body: some View {
        NavigationStack {
            MainContentView(
                pdfDocument: $pdfDocument,
                currentPage: $currentPage,
                resetZoom: $resetZoom,
                zoomedIn: $zoomedIn,
                annotationsEnabled: $annotationsEnabled,
                pagePaths: $pagePaths,
                highlightPaths: $highlightPaths,
                selectedScribbleTool: $selectedScribbleTool,
                textAnnotations: $textAnnotations,
                showDigitalResources: $showDigitalResources,
                showClearAlert: $showClearAlert,
                showingFeedback: $showingFeedback,
                timerManager: timerManager,
                annotationManager: annotationManager,
                exitNotSelected: $exitNotSelected,
                covers: $covers,
                isCurrentPageBookmarked: isCurrentPageBookmarked,
                toggleCurrentPageInBookmarks: toggleCurrentPageInBookmarks,
                clearMarkup: clearMarkup,
                pageChangeEnabled: $pageChangeEnabled,
                goToNextPage: goToNextPage,
                goToPreviousPage: goToPreviousPage,
                loadPathsForPage: loadPathsForPage,
                loadPDFFromURL: loadPDFFromURL,
                fileName: $fileName
            )
        }
    }

    private func dragGesture() -> some Gesture {
        if pageChangeEnabled, !zoomedIn {
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
        // Load paths from AnnotationManager, if available
        pagePaths[key] = pagePaths[key] ?? []
        highlightPaths[key] = highlightPaths[key] ?? []
        textAnnotations[key] = textAnnotations[key] ?? []
    }

    private func uniqueKey(for pageIndex: Int) -> String {
        guard let fileName = fileName else { return "\(pageIndex)" }
        return "\(fileName)-\(pageIndex)"
    }

    private func clearMarkup() {
        highlightPaths.removeValue(forKey: uniqueKey(for: currentPage))
        pagePaths.removeValue(forKey: uniqueKey(for: currentPage))
        textAnnotations.removeValue(forKey: uniqueKey(for: currentPage))
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

// Subview for the main content
struct MainContentView: View {
    @Binding var pdfDocument: PDFDocument?
    @Binding var currentPage: Int
    @Binding var resetZoom: Bool
    @Binding var zoomedIn: Bool
    @Binding var annotationsEnabled: Bool
    @Binding var pagePaths: [String: [Path]]
    @Binding var highlightPaths: [String: [Path]]
    @Binding var selectedScribbleTool: String
    @Binding var textAnnotations: [String: [TextAnnotation]]
    @Binding var showDigitalResources: Bool
    @Binding var showClearAlert: Bool
    @Binding var showingFeedback: Bool
    
    @ObservedObject var timerManager: TimerManager
    @ObservedObject var annotationManager: AnnotationManager
    
    @Binding var exitNotSelected: Bool
    @Binding var covers: [Cover]?

    var isCurrentPageBookmarked: Bool
    var toggleCurrentPageInBookmarks: () -> Void
    var clearMarkup: () -> Void
    @Binding var pageChangeEnabled: Bool
    var goToNextPage: () -> Void
    var goToPreviousPage: () -> Void
    var loadPathsForPage: (Int) -> Void
    var loadPDFFromURL: () -> Void

    @Binding var fileName: String?

    var body: some View {
        ZStack {
            VStack {
                if let pdfDocument = pdfDocument {
                    PDFLoadedView(
                        pdfDocument: pdfDocument,
                        currentPage: $currentPage,
                        resetZoom: $resetZoom,
                        zoomedIn: $zoomedIn,
                        annotationsEnabled: $annotationsEnabled,
                        pagePaths: $pagePaths,
                        highlightPaths: $highlightPaths,
                        selectedScribbleTool: $selectedScribbleTool,
                        textAnnotations: $textAnnotations,
                        pageChangeEnabled: $pageChangeEnabled,
                        goToNextPage: goToNextPage,
                        goToPreviousPage: goToPreviousPage,
                        loadPathsForPage: loadPathsForPage
                    )
                    .toolbar {
                        ToolbarItemGroup(placement: .navigationBarTrailing) {
                            TimerControlsView(timerManager: timerManager)
                            MarkupControlsView(
                                selectedScribbleTool: $selectedScribbleTool,
                                annotationsEnabled: $annotationsEnabled,
                                exitNotSelected: $exitNotSelected,
                                showClearAlert: $showClearAlert,
                                clearMarkup: clearMarkup,
                                saveAnnotations: {
                                    annotationManager.saveAnnotations(
                                        pagePaths: pagePaths,
                                        highlightPaths: highlightPaths,
                                        textAnnotations: textAnnotations
                                    )
                                }
                            )
                            DigitalResourcesButton(showDigitalResources: $showDigitalResources, covers: covers)
                            BookmarkButton(isBookmarked: isCurrentPageBookmarked, toggleBookmark: toggleCurrentPageInBookmarks)
                            if zoomedIn {
                                Button("Reset Zoom") { resetZoom = true }
                            }
                        }
                        ToolbarItem(placement: .bottomBar) {
                            ToolbarBottomBarView(timerManager: timerManager, showingFeedback: $showingFeedback)
                        }
                    }
                } else {
                    PDFLoadingView(
                        loadPDFFromURL: loadPDFFromURL,
                        annotationManager: annotationManager,
                        pagePaths: $pagePaths,
                        highlightPaths: $highlightPaths,
                        textAnnotations: $textAnnotations,
                        annotationsEnabled: $annotationsEnabled
                    )
                }
            }
        }
        .alert("Are you sure you want to clear your screen?", isPresented: $showClearAlert) {
            Button("Clear", role: .destructive) { clearMarkup() }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(isPresented: $showingFeedback) {
            FeedbackView()
        }
        .onChange(of: fileName) { _ in
            loadPDFFromURL()
        }
    }
}

// Subview for when the PDF is loaded
struct PDFLoadedView: View {
    let pdfDocument: PDFDocument
    @Binding var currentPage: Int
    @Binding var resetZoom: Bool
    @Binding var zoomedIn: Bool
    @Binding var annotationsEnabled: Bool
    @Binding var pagePaths: [String: [Path]]
    @Binding var highlightPaths: [String: [Path]]
    @Binding var selectedScribbleTool: String
    @Binding var textAnnotations: [String: [TextAnnotation]]
    @Binding var pageChangeEnabled: Bool
    
    var goToNextPage: () -> Void
    var goToPreviousPage: () -> Void
    var loadPathsForPage: (Int) -> Void

    var body: some View {
        ZStack {
            DocumentView(
                pdfDocument: pdfDocument,
                currentPageIndex: $currentPage,
                resetZoom: $resetZoom,
                zoomedIn: $zoomedIn
            )
            .edgesIgnoringSafeArea(.all)
            .gesture(dragGesture())
            .onChange(of: currentPage) { _ in
                loadPathsForPage(currentPage)
            }
            
            if annotationsEnabled {
                AnnotationsView(
                    pagePaths: $pagePaths,
                    highlightPaths: $highlightPaths,
                    key: uniqueKey(for: currentPage),
                    selectedScribbleTool: $selectedScribbleTool,
                    textAnnotations: $textAnnotations,
                    nextPage: goToNextPage,
                    previousPage: goToPreviousPage
                )
            }
        }
    }
    
    private func uniqueKey(for pageIndex: Int) -> String {
        guard let documentURL = pdfDocument.documentURL else { return "\(pageIndex)" }
        let fileName = documentURL.deletingPathExtension().lastPathComponent
        return "\(fileName)-\(pageIndex)"
    }

    private func dragGesture() -> some Gesture {
        if pageChangeEnabled, !zoomedIn {
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
}

// Subview for when the PDF is loading
struct PDFLoadingView: View {
    var loadPDFFromURL: () -> Void
    @ObservedObject var annotationManager: AnnotationManager
    @Binding var pagePaths: [String: [Path]]
    @Binding var highlightPaths: [String: [Path]]
    @Binding var textAnnotations: [String: [TextAnnotation]]
    @Binding var annotationsEnabled: Bool

    var body: some View {
        ProgressView("Getting Workbook")
            .onAppear {
                loadPDFFromURL()
                annotationManager.loadAnnotations(
                    pagePaths: &pagePaths,
                    highlightPaths: &highlightPaths,
                    textAnnotations: &textAnnotations
                )
                if !pagePaths.isEmpty || !highlightPaths.isEmpty {
                    annotationsEnabled = true
                }
            }
    }
}

// Subview for Timer Controls
struct TimerControlsView: View {
    @ObservedObject var timerManager: TimerManager

    var body: some View {
        // Wrap the conditional logic in a Group
        Group {
            if timerManager.isTimerRunning {
                TimerRunningControls(timerManager: timerManager)
            } else if timerManager.isPaused {
                TimerPausedControls(timerManager: timerManager)
            } else {
                TimerMenu(timerManager: timerManager)
            }
        }
    }
}

// Subview for Timer Running State
struct TimerRunningControls: View {
    @ObservedObject var timerManager: TimerManager

    var body: some View {
        HStack {
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
        }
    }
}

// Subview for Timer Paused State
struct TimerPausedControls: View {
    @ObservedObject var timerManager: TimerManager

    var body: some View {
        HStack {
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
        }
    }
}

// Subview for Timer Menu
struct TimerMenu: View {
    @ObservedObject var timerManager: TimerManager

    var body: some View {
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

// Subview for Markup Controls
struct MarkupControlsView: View {
    @Binding var selectedScribbleTool: String
    @Binding var annotationsEnabled: Bool
    @Binding var exitNotSelected: Bool
    @Binding var showClearAlert: Bool
    
    var clearMarkup: () -> Void
    var saveAnnotations: () -> Void
    
    @ObservedObject private var annotationManager = AnnotationManager()

    var body: some View {
        Menu {
            Button("Pen") {
                selectScribbleTool("Pen")
            }
            Button("Highlight") {
                selectScribbleTool("Highlight")
            }
            Button("Erase") {
                selectScribbleTool("Erase")
            }
            Button("Text") {
                selectScribbleTool("Text")
            }
            Button("Clear Screen") {
                showClearAlert = true
            }
            Button("Exit Markup") {
                selectScribbleTool("")
                exitNotSelected = false
                saveAnnotations()
            }
        } label: {
            Text(selectedScribbleTool.isEmpty ? "Markup" : "Markup: " + selectedScribbleTool)
                .padding(5)
                .foregroundColor(exitNotSelected ? Color.pink : Color.blue)
                .cornerRadius(8)
        }
        .onChange(of: selectedScribbleTool) { _ in
            if !selectedScribbleTool.isEmpty {
                annotationsEnabled = true
                exitNotSelected = true
            }
        }
    }

    private func selectScribbleTool(_ tool: String) {
        selectedScribbleTool = tool
    }
}

// Subview for Digital Resources Button
struct DigitalResourcesButton: View {
    @Binding var showDigitalResources: Bool
    var covers: [Cover]?

    var body: some View {
        Button(action: {
            showDigitalResources = true
        }) {
            Text("Digital Resources")
                .padding(5)
                .foregroundColor((covers?.isEmpty ?? true) ? .gray : .purple)
                .cornerRadius(8)
        }
        .disabled(covers?.isEmpty ?? true)
        .fullScreenCover(isPresented: $showDigitalResources) {
            DigitalResourcesView(covers: covers)
        }
    }
}

// Subview for Bookmark Button
struct BookmarkButton: View {
    var isBookmarked: Bool
    var toggleBookmark: () -> Void

    var body: some View {
        Button {
            toggleBookmark()
        } label: {
            Image(systemName: isBookmarked ? "bookmark.fill" : "bookmark")
                .foregroundColor(.yellow)
        }
    }
}

// Subview for Bottom Toolbar
struct ToolbarBottomBarView: View {
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

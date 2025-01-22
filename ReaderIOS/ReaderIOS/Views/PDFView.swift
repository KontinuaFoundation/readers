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

    @State private var currentZoom: CGFloat = 0.0
    @State private var totalZoom: CGFloat = 1.2
    @State private var zoomedIn: Bool = false
    @State private var showingFeedback = false

    @ObservedObject private var timerManager = TimerManager()
    @State private var annotationsEnabled: Bool = false
    @State private var exitNotSelected: Bool = false
    @State private var selectedScribbleTool: String = ""
    @State private var pageChangeEnabled: Bool = true
    @State private var pagePaths: [String: [Path]] = [:]
    @State private var highlightPaths: [String: [Path]] = [:]

    @State private var showClearAlert = false
    @ObservedObject private var annotationManager = AnnotationManager()

    var body: some View {
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
                            .scaleEffect(max(min(currentZoom + totalZoom, 5.0), 1.2))
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
                                    nextPage: { goToNextPage() },
                                    previousPage: { goToPreviousPage() },
                                    annotationManager: annotationManager,
                                    currentZoom: $currentZoom,
                                    totalZoom: $totalZoom
                                )
                                .scaleEffect(max(min(currentZoom + totalZoom, 5.0), 1.2))
                            }
                        }
                        .toolbar {
                            ToolbarItemGroup(placement: .navigationBarTrailing) {
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
                                Menu {
                                    Button("Pen") {
                                        selectScribbleTool("Pen")
                                        annotationsEnabled = true
                                        exitNotSelected = true
                                    }
                                    Button("Highlight") {
                                        selectScribbleTool("Highlight")
                                        annotationsEnabled = true
                                        exitNotSelected = true
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

                                if zoomedIn {
                                    Button("Reset Zoom") {}
                                }
                            }

                            ToolbarItem(placement: .bottomBar) {
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

                //                VStack {
                //                    Spacer()
                //                    HStack {
                //                        Spacer()
                //                        Button {
                //                            showingFeedback = true
                //                        } label: {
                //                            Image(systemName: "message.fill")
                //                                .font(.system(size: 24))
                //                                .foregroundColor(.white)
                //                                .padding(15)
                //                                .background(Color.blue)
                //                                .clipShape(Circle())
                //                                .shadow(radius: 4)
                //                        }
                //                        .padding(.trailing, 70)
                //                        .padding(.bottom, 30)
                //                    }
                //                }
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
        .onChange(of: fileName) { _ in
            loadPDFFromURL()
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
    NavigationPDFSplitView()
}

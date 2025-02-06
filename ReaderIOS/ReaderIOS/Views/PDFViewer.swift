import PDFKit
import SwiftUI

struct URLItem: Identifiable {
    let id = UUID()
    let url: URL
}

struct PDFViewer: View {
    @StateObject private var viewModel = PDFViewModel()
    @Binding var fileName: String?
    @Binding var covers: [Cover]?
    @Binding var currentPage: Int

    @StateObject private var zoomManager = ZoomManager()
    @StateObject private var timerManager = TimerManager()
    @StateObject private var annotationManager = AnnotationManager()

    // Local UI state
    @State private var showDigitalResources = false
    @State private var showingFeedback = false
    @State private var annotationsEnabled = false
    @State private var exitNotSelected = false
    @State private var selectedScribbleTool = ""
    @State private var selectedPenColor: Color = .black
    @State private var selectedHighlighterColor: Color = .yellow
    @State private var isPenSubmenuVisible = false
    @State private var showClearAlert = false

    var body: some View {
        GeometryReader { geometry in
            NavigationStack {
                ZStack {
                    VStack {
                        if let pdfDocument = viewModel.pdfDocument {
                            ZoomableContent(
                                pdfDocument: pdfDocument,
                                currentPage: $viewModel.currentPage,
                                annotationsEnabled: annotationsEnabled,
                                pagePaths: $viewModel.pagePaths,
                                highlightPaths: $viewModel.highlightPaths,
                                zoomManager: zoomManager,
                                geometrySize: geometry.size,
                                annotationManager: annotationManager,
                                selectedScribbleTool: $selectedScribbleTool,
                                selectedPenColor: selectedPenColor,
                                selectedHighlighterColor: selectedHighlighterColor
                            )
                            .onChange(of: viewModel.currentPage) { newValue in
                                viewModel.loadPathsForPage(newValue)
                            }
                            .toolbar {
                                ToolbarItemGroup(placement: .navigationBarTrailing) {
                                    TimerMenuView(timerManager: timerManager)
                                    MarkupMenuView(
                                        selectedScribbleTool: $selectedScribbleTool,
                                        annotationsEnabled: $annotationsEnabled,
                                        exitNotSelected: $exitNotSelected,
                                        showClearAlert: $showClearAlert,
                                        selectedPenColor: $selectedPenColor,
                                        selectedHighlighterColor: $selectedHighlighterColor,
                                        isPenSubmenuVisible: $isPenSubmenuVisible,
                                        annotationManager: annotationManager,
                                        pagePaths: viewModel.pagePaths,
                                        highlightPaths: viewModel.highlightPaths
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
                                        viewModel.toggleCurrentPageInBookmarks()
                                    } label: {
                                        Image(systemName: viewModel
                                            .isCurrentPageBookmarked ? "bookmark.fill" : "bookmark")
                                            .foregroundColor(.yellow)
                                    }

                                    if zoomManager.zoomedIn {
                                        Button("Reset Zoom") {
                                            zoomManager.resetZoom()
                                        }
                                    }
                                }
                                ToolbarItem(placement: .bottomBar) {
                                    TimerProgressBarView(timerManager: timerManager, showingFeedback: $showingFeedback)
                                }
                            }
                        } else {
                            ProgressView("Getting Workbook")
                                .onAppear {
                                    viewModel.fileName = fileName
                                    viewModel.loadPDFFromURL()
                                    annotationManager.loadAnnotations(
                                        pagePaths: &viewModel.pagePaths,
                                        highlightPaths: &viewModel.highlightPaths
                                    )
                                    if !viewModel.pagePaths.isEmpty || !viewModel.highlightPaths.isEmpty {
                                        annotationsEnabled = true
                                    }
                                }
                        }
                    }
                }
            }
            .alert("Are you sure you want to clear your screen?", isPresented: $showClearAlert) {
                Button("Clear", role: .destructive) {
                    viewModel.clearMarkup()
                    annotationManager.saveAnnotations(
                        pagePaths: viewModel.pagePaths,
                        highlightPaths: viewModel.highlightPaths
                    )
                }
                Button("Cancel", role: .cancel) {}
            }
            .sheet(isPresented: $showingFeedback) {
                FeedbackView()
            }
            // If fileName is externally bound, you can listen for changes:
            .onChange(of: viewModel.fileName) { _, _ in
                viewModel.loadPDFFromURL()
            }
        }
    }
}

//
//  ZoomableContent.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import PDFKit
import SwiftUI

struct ZoomableContent: View {
    let pdfDocument: PDFDocument
    @Binding var currentPage: Int
    let annotationsEnabled: Bool
    @Binding var pagePaths: [String: [(path: Path, color: Color)]]
    @Binding var highlightPaths: [String: [(path: Path, color: Color)]]
    @ObservedObject var zoomManager: ZoomManager
    let geometrySize: CGSize
    let annotationManager: AnnotationManager
    @Binding var selectedScribbleTool: String
    let selectedPenColor: Color
    let selectedHighlighterColor: Color

    var body: some View {
        ZStack {
            DocumentView(
                pdfDocument: pdfDocument,
                currentPageIndex: $currentPage
            )
            .edgesIgnoringSafeArea(.all)
            .modifier(ZoomableModifier(zoomManager: zoomManager, geometrySize: geometrySize))

            if annotationsEnabled {
                AnnotationsView(
                    pagePaths: $pagePaths,
                    highlightPaths: $highlightPaths,
                    key: "\(currentPage)",
                    selectedScribbleTool: $selectedScribbleTool,
                    nextPage: { /* Implement if needed */ },
                    previousPage: { /* Implement if needed */ },
                    annotationManager: annotationManager,
                    selectedColor: selectedPenColor,
                    selectedHighlighterColor: selectedHighlighterColor,
                    zoomedIn: zoomManager.zoomedIn
                )
                .modifier(ZoomableModifier(zoomManager: zoomManager, geometrySize: geometrySize))
            }
        }
    }
}

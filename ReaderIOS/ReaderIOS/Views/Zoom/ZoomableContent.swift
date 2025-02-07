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
                    nextPage: {
                        if currentPage < pdfDocument.pageCount - 1 {
                            currentPage += 1
                        }
                    },
                    previousPage: {
                        if currentPage > 0 {
                            currentPage -= 1
                        }
                    },
                    annotationManager: annotationManager,
                    selectedColor: selectedPenColor,
                    selectedHighlighterColor: selectedHighlighterColor,
                    zoomedIn: zoomManager.zoomedIn
                )
                .allowsHitTesting(selectedScribbleTool != "")
            }
        }
        .gesture(
            DragGesture(minimumDistance: 20)
                .onEnded { value in
                    if !zoomManager.zoomedIn && selectedScribbleTool.isEmpty {
                        if value.translation.width < 0 && currentPage < pdfDocument.pageCount - 1 {
                            currentPage += 1
                        } else if value.translation.width > 0 && currentPage > 0 {
                            currentPage -= 1
                        }
                    }
                }
        )
    }
}

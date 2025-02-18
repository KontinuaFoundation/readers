import PDFKit
import SwiftUI

struct PathWithColor {
    var path: Path
    var color: Color
}

struct AnnotationsView: View {
    @Binding var pagePaths: [String: [(path: Path, color: Color)]]
    @Binding var highlightPaths: [String: [(path: Path, color: Color)]]

    var key: String
    @Binding var selectedScribbleTool: String
    var nextPage: (() -> Void)?
    var previousPage: (() -> Void)?
    @State private var liveDrawingPath: Path = .init()
    @State private var liveDrawingColor: Color = .black // pen color default
    @State private var liveHighlighterColor: Color = .yellow // highlight color default
    @ObservedObject var annotationManager: AnnotationManager
    @ObservedObject var textManager: TextManager
    @Binding var textBoxes: [String: [TextBoxData]]
    var selectedColor: Color
    var selectedHighlighterColor: Color
    var zoomedIn: Bool
    @Binding var textOpened: Bool

    var body: some View {
        Canvas { context, _ in
            // Existing page paths have their old colors
            if let paths = pagePaths[key] {
                for pathInfo in paths {
                    context.stroke(Path(pathInfo.path.cgPath), with: .color(pathInfo.color), lineWidth: 2)
                }
            }

            if let hPaths = highlightPaths[key] {
                for pathInfo in hPaths {
                    context.stroke(pathInfo.path, with: .color(pathInfo.color.opacity(0.2)), lineWidth: 15)
                }
            }

            // Live drawing path (use liveDrawingColor)
            context.stroke(Path(liveDrawingPath.cgPath),
                           with: selectedScribbleTool == "Highlight" ? .color(liveHighlighterColor.opacity(0.2)) :
                               .color(liveDrawingColor),
                           lineWidth: selectedScribbleTool == "Highlight" ? 15 : 2)
        }
        .gesture(
            DragGesture(minimumDistance: 0.0001)
                .onChanged { value in
                    if selectedScribbleTool == "Erase" {
                        erasePath(at: value.location)
                    } else if selectedScribbleTool == "Pen" || selectedScribbleTool == "Highlight" {
                        updateLivePath(with: value.location)
                    }
                }
                .onEnded { value in
                    if selectedScribbleTool == "Pen" {
                        finalizeCurrentPath(for: &pagePaths, color: liveDrawingColor)
                    } else if selectedScribbleTool == "Highlight" {
                        finalizeCurrentPath(for: &highlightPaths, color: liveDrawingColor)
                    } else if selectedScribbleTool == "" || selectedScribbleTool == "Text", !zoomedIn {
                        if value.translation.width < 0 {
                            nextPage?()
                        } else if value.translation.width > 0 {
                            previousPage?()
                        }
                        textOpened = false
                    }
                    annotationManager.saveAnnotations(
                        pagePaths: pagePaths,
                        highlightPaths: highlightPaths
                    )
                    textManager.saveTextBoxes(textBoxes: textBoxes)
                }
        )
        .onAppear {
            liveDrawingColor = selectedColor
            liveHighlighterColor = selectedHighlighterColor
        }
        .onChange(of: selectedColor) { newColor, _ in
            liveDrawingColor = newColor
        }
        .onChange(of: selectedHighlighterColor) { newColor, _ in
            liveHighlighterColor = newColor
        }
    }

    private func erasePath(at location: CGPoint) {
        if let pagePathsForCurrentPage = pagePaths[key] {
            for (index, pathInfo) in pagePathsForCurrentPage.enumerated() where pathInfo.path.contains(location) {
                pagePaths[key]?.remove(at: index)
                break
            }
        }
        if let highlightPathsForCurrentPage = highlightPaths[key] {
            for (index, pathInfo) in highlightPathsForCurrentPage.enumerated() where pathInfo.path.contains(location) {
                highlightPaths[key]?.remove(at: index)
                break
            }
        }
    }

    private func updateLivePath(with point: CGPoint) {
        if liveDrawingPath.isEmpty {
            liveDrawingPath.move(to: point)
        } else {
            liveDrawingPath.addLine(to: point)
        }
    }

    private func finalizeCurrentPath(for pathDirectory: inout [String: [(path: Path, color: Color)]], color _: Color) {
        if !liveDrawingPath.isEmpty {
            let pathColor = selectedScribbleTool == "Highlight" ? liveHighlighterColor : liveDrawingColor
            pathDirectory[key, default: []].append((path: liveDrawingPath, color: pathColor))
            liveDrawingPath = Path()
        }
    }
}

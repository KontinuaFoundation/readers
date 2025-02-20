import PDFKit
import SwiftUI

struct AnnotationsView: View {
    // MARK: - Bindings

    @Binding var pagePaths: [String: [(path: Path, color: Color)]]
    @Binding var highlightPaths: [String: [(path: Path, color: Color)]]
    @Binding var selectedScribbleTool: String
    @Binding var textOpened: Bool

    // MARK: - Input Parameters

    let key: String
    let nextPage: (() -> Void)?
    let previousPage: (() -> Void)?
    let selectedColor: Color
    let selectedHighlighterColor: Color
    let zoomedIn: Bool
    let zoomManager: ZoomManager

    // MARK: - Observed Objects

    @ObservedObject var annotationManager: AnnotationStorageManager
    @ObservedObject var textManager: TextManager

    // MARK: - Binding Data

    @Binding var textBoxes: [String: [TextBoxData]]

    // MARK: - Local State

    @State private var liveDrawingPath: Path = .init()
    @State private var liveDrawingColor: Color = .black
    @State private var liveHighlighterColor: Color = .yellow

    // MARK: - Body

    var body: some View {
        GeometryReader { geometry in
            Canvas { context, _ in
                // Draw saved paths
                if let paths = pagePaths[key] {
                    for pathInfo in paths {
                        context.stroke(Path(pathInfo.path.cgPath),
                                       with: .color(pathInfo.color),
                                       lineWidth: 2)
                    }
                }
                if let hPaths = highlightPaths[key] {
                    for pathInfo in hPaths {
                        context.stroke(pathInfo.path,
                                       with: .color(pathInfo.color.opacity(0.2)),
                                       lineWidth: 15)
                    }
                }
                // Draw live drawing path
                context.stroke(Path(liveDrawingPath.cgPath),
                               with: selectedScribbleTool == "Highlight" ?
                                   .color(liveHighlighterColor.opacity(0.2)) :
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
                            finalizeCurrentPath(for: &pagePaths, using: liveDrawingColor)
                        } else if selectedScribbleTool == "Highlight" {
                            finalizeCurrentPath(for: &highlightPaths, using: liveHighlighterColor)
                        } else if selectedScribbleTool.isEmpty || selectedScribbleTool == "Text", !zoomedIn {
                            if value.translation.width < 0 { nextPage?() }
                            else if value.translation.width > 0 { previousPage?() }
                            textOpened = false
                        }
                        annotationManager.saveAnnotations(pagePaths: pagePaths, highlightPaths: highlightPaths)
                        textManager.saveTextBoxes(textBoxes: textBoxes)
                    }
            )
            .simultaneousGesture(zoomManager.zoomin())
            .simultaneousGesture(zoomManager.zoomout())
            .onTapGesture(count: 1, coordinateSpace: .local) { location in
                zoomManager.newZoomPoint(newPoint: location,
                                         width: geometry.size.width,
                                         height: geometry.size.height)
                if !textOpened, selectedScribbleTool == "Text" {
                    textOpened = true
                    textManager.addText(textBoxes: $textBoxes,
                                        key: key,
                                        width: geometry.size.width,
                                        height: geometry.size.height)
                    textManager.saveTextBoxes(textBoxes: textBoxes)
                } else {
                    textOpened = false
                }
            }
        }
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

    // MARK: - Private Helpers

    private func erasePath(at location: CGPoint) {
        if let paths = pagePaths[key] {
            for (index, pathInfo) in paths.enumerated() where pathInfo.path.contains(location) {
                pagePaths[key]?.remove(at: index)
                break
            }
        }
        if let hPaths = highlightPaths[key] {
            for (index, pathInfo) in hPaths.enumerated() where pathInfo.path.contains(location) {
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

    private func finalizeCurrentPath(for paths: inout [String: [(path: Path, color: Color)]],
                                     using _: Color)
    {
        guard !liveDrawingPath.isEmpty else { return }
        let finalColor = selectedScribbleTool == "Highlight" ? liveHighlighterColor : liveDrawingColor
        paths[key, default: []].append((path: liveDrawingPath, color: finalColor))
        liveDrawingPath = Path()
    }
}

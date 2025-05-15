import PDFKit
import SwiftUI

enum TapConstants {
    static let prevPageTapRatio = 0.25
}

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

    @State private var lastDragValue: CGSize = .zero

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
                        if zoomManager.getZoomedIn(), selectedScribbleTool.isEmpty {
                            // Calculate the incremental translation since the last update.
                            let delta = CGSize(
                                width: value.translation.width - lastDragValue.width,
                                height: value.translation.height - lastDragValue.height
                            )
                            zoomManager.panZoomCenter(by: delta, in: geometry.size)
                            lastDragValue = value.translation
                        } else {
                            // Existing logic for drawing/erasing
                            if selectedScribbleTool == "Erase" {
                                erasePath(at: value.location)
                            } else if selectedScribbleTool == "Pen" || selectedScribbleTool == "Highlight" {
                                updateLivePath(with: value.location)
                            }
                        }
                    }
                    .onEnded { value in
                        // Reset the incremental drag offset
                        lastDragValue = .zero

                        if !zoomManager.getZoomedIn() || !selectedScribbleTool.isEmpty {
                            if selectedScribbleTool == "Pen" {
                                finalizeCurrentPath(for: &pagePaths, using: liveDrawingColor)
                            } else if selectedScribbleTool == "Highlight" {
                                finalizeCurrentPath(for: &highlightPaths, using: liveHighlighterColor)
                            } else if selectedScribbleTool.isEmpty || selectedScribbleTool == "Text" {
                                // Page change logic (swipe left/right) only when not zoomed in.
                                if value.translation.width < 0 {
                                    nextPage?()
                                } else if value.translation.width > 0 {
                                    previousPage?()
                                }
                                textOpened = false
                            }
                            annotationManager.saveAnnotations(pagePaths: pagePaths, highlightPaths: highlightPaths)
                            textManager.saveTextBoxes(textBoxes: textBoxes)
                        }
                    }
            )
            // Use the updated zoomManager gestures
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
                    return
                }
                // else
                textOpened = false

                // ——— PAGE TURNS ———
                guard selectedScribbleTool.isEmpty else { return }
                if !zoomManager.getZoomedIn() {
                    if location.x > geometry.size.width * TapConstants.prevPageTapRatio {
                        nextPage?()
                    } else {
                        previousPage?()
                    }
                }
            }
        }
        .onAppear {
            // Initialize color values
            liveDrawingColor = selectedColor
            liveHighlighterColor = selectedHighlighterColor
        }
        .onChange(of: selectedColor) { newColor, _ in
            liveDrawingColor = newColor
        }
        .onChange(of: selectedHighlighterColor) { newColor, _ in
            liveHighlighterColor = newColor
        }
        // Listen for geometry changes to update zoom manager
        .background(GeometryReader { geometry in
            Color.clear
                .onAppear {
                    zoomManager.updateViewSize(geometry.size)
                }
                .onChange(of: geometry.size) { _, newSize in
                    zoomManager.updateViewSize(newSize)
                }
        })
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

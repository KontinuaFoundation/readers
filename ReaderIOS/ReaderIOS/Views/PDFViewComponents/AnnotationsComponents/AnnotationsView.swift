import PDFKit
import SwiftUI

enum TapConstants {
    static let prevPageTapRatio = 0.25
}

struct AnnotationsView: View {
    // MARK: - Bindings

    @Binding var pagePaths: [String: [(path: Path, color: Color)]]
    @Binding var highlightPaths: [String: [(path: Path, color: Color)]]
    @Binding var selectedScribbleTool: AnnotationMode
    @Binding var textOpened: Bool

    // MARK: - Input Parameters

    let key: String
    let nextPage: (() -> Void)?
    let previousPage: (() -> Void)?
    @Binding var selectedColor: Color
    @Binding var selectedHighlighterColor: Color
    let zoomedIn: Bool
    let zoomManager: ZoomManager

    // MARK: - Observed Objects

    @ObservedObject var annotationManager: AnnotationStorageManager
    @ObservedObject var textManager: TextManager

    // MARK: - Binding Data

    @Binding var textBoxes: [String: [TextBoxData]]
    @Binding var isHidden: Bool
    let pageFrame: CGRect

    // MARK: - Local State

    @State private var liveDrawingPath: Path = .init()

    @State private var lastDragValue: CGSize = .zero

    // MARK: - Body

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .bottomLeading) {
                Canvas { context, _ in
                    if !isHidden {
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
                                       with: selectedScribbleTool == .highlight ?
                                           .color(selectedHighlighterColor.opacity(0.2)) :
                                           .color(selectedColor),
                                       lineWidth: selectedScribbleTool == .highlight ? 15 : 2)
                    }
                }
                .gesture(
                    DragGesture(minimumDistance: 0.0001)
                        .onChanged { value in
                            if zoomManager.getZoomedIn(), selectedScribbleTool == .none {
                                // Calculate the incremental translation since the last update.
                                let delta = CGSize(
                                    width: value.translation.width - lastDragValue.width,
                                    height: value.translation.height - lastDragValue.height
                                )
                                zoomManager.panZoomCenter(by: delta, in: geometry.size)
                                lastDragValue = value.translation
                            } else {
                                // Existing logic for drawing/erasing
                                if isHidden { return }
                                if selectedScribbleTool == .erase {
                                    erasePath(at: value.location)
                                } else if selectedScribbleTool == .pen || selectedScribbleTool == .highlight {
                                    updateLivePath(with: value.location)
                                }
                            }
                        }
                        .onEnded { value in
                            // Reset the incremental drag offset
                            lastDragValue = .zero

                            if !zoomManager.getZoomedIn() || selectedScribbleTool != .none {
                                if selectedScribbleTool == .none || selectedScribbleTool == .text || isHidden {
                                    // Page change logic (swipe left/right) only when not zoomed in.
                                    if value.translation.width < 0 {
                                        nextPage?()
                                    } else if value.translation.width > 0 {
                                        previousPage?()
                                    }
                                    textOpened = false
                                } else if selectedScribbleTool == .pen {
                                    finalizeCurrentPath(for: &pagePaths, using: selectedColor)
                                } else if selectedScribbleTool == .highlight {
                                    finalizeCurrentPath(for: &highlightPaths, using: selectedHighlighterColor)
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

                    if !textOpened, selectedScribbleTool == .text {
                        textOpened = true
                        textManager.addText(textBoxes: $textBoxes,
                                            key: key,
                                            width: geometry.size.width,
                                            height: geometry.size.height)
                        textManager.saveTextBoxes(textBoxes: textBoxes)
                        return
                    }
                    // else
                    if textOpened {
                        textOpened = false
                        return
                    }

                    // ——— PAGE TURNS ———
                    guard selectedScribbleTool == .none else { return }
                    if !zoomManager.getZoomedIn() {
                        if location.x > geometry.size.width * TapConstants.prevPageTapRatio {
                            nextPage?()
                        } else {
                            previousPage?()
                        }
                    }
                }
                if isHidden || textBoxes[key]?.count ?? 0 > 0 || pagePaths[key]?.count ?? 0 > 0 || highlightPaths[key]?
                    .count ?? 0 > 0
                {
                    Image(systemName: isHidden ? "eye.slash" : "eye")
                        .foregroundStyle(Color.blue)
                        .font(.system(size: 25))
                        .padding()
                        .onTapGesture {
                            isHidden.toggle()
                        }
                        .zIndex(999)
                }
            }
            .frame(width: pageFrame.width, height: pageFrame.height)
            .offset(x: pageFrame.origin.x, y: pageFrame.origin.y)
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
        .overlay(alignment: .bottomLeading) {
            if textBoxes[key]?.count ?? 0 > 0 || isHidden
                || pagePaths[key]?.count ?? 0 > 0
                || highlightPaths[key]?.count ?? 0 > 0
            {
                Image(systemName: isHidden ? "eye.slash" : "eye")
                    .foregroundStyle(Color.blue)
                    .font(.system(size: 25))
                    .padding()
                    .onTapGesture {
                        isHidden.toggle()
                    }
                    .zIndex(999)
            }
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
        let finalColor = selectedScribbleTool == .highlight ? selectedHighlighterColor : selectedColor
        paths[key, default: []].append((path: liveDrawingPath, color: finalColor))
        liveDrawingPath = Path()
    }
}

import PDFKit
import SwiftUI

struct AnnotationsView: View {
    @Binding var pagePaths: [String: [Path]]
    @Binding var highlightPaths: [String: [Path]]
    var key: String
    @Binding var selectedScribbleTool: String
    var nextPage: (() -> Void)?
    var previousPage: (() -> Void)?

    // Color and Size Properties
    let penColor: Color
    let penSize: CGFloat
    let highlighterColor: Color
    let highlighterSize: CGFloat

    @State private var liveDrawingPath: Path = .init()

    var body: some View {
        Canvas { context, _ in
            // Draw saved pen paths
            if let paths = pagePaths[key] {
                for path in paths {
                    context.stroke(Path(path.cgPath), with: .color(penColor), style: StrokeStyle(lineWidth: penSize, lineCap: .round, lineJoin: .round))
                }
            }
            
            // Draw saved highlighter paths
            if let hPaths = highlightPaths[key] {
                for path in hPaths {
                    context.stroke(Path(path.cgPath), with: .color(highlighterColor.opacity(0.4)), style: StrokeStyle(lineWidth: highlighterSize, lineCap: .round, lineJoin: .round))
                }
            }
            
            // Draw the live path
            if selectedScribbleTool == "Highlight" {
                context.stroke(Path(liveDrawingPath.cgPath), with: .color(highlighterColor.opacity(0.4)), style: StrokeStyle(lineWidth: highlighterSize, lineCap: .round, lineJoin: .round))
            } else if selectedScribbleTool == "Pen" {
                context.stroke(Path(liveDrawingPath.cgPath), with: .color(penColor), style: StrokeStyle(lineWidth: penSize, lineCap: .round, lineJoin: .round))
            }
        }
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    if selectedScribbleTool == "Erase" {
                        erasePath(at: value.location)
                    } else if selectedScribbleTool == "Pen" || selectedScribbleTool == "Highlight" {
                        updateLivePath(with: value.location)
                    }
                }
                .onEnded { value in
                    if selectedScribbleTool == "Pen" {
                        finalizeCurrentPath(for: &pagePaths)
                    } else if selectedScribbleTool == "Highlight" {
                        finalizeCurrentPath(for: &highlightPaths)
                    } else if selectedScribbleTool == "" {
                        // Page navigation gestures
                        if value.translation.width < 0 {
                            nextPage?()
                        } else if value.translation.width > 0 {
                            previousPage?()
                        }
                    }
                }
        )
    }

    private func erasePath(at location: CGPoint) {
        // Iterate through pen paths
        if var paths = pagePaths[key] {
            for (index, path) in paths.enumerated().reversed() {
                if path.contains(location) {
                    pagePaths[key]?.remove(at: index)
                }
            }
        }
        
        // Iterate through highlighter paths
        if var hPaths = highlightPaths[key] {
            for (index, path) in hPaths.enumerated().reversed() {
                if path.contains(location) {
                    highlightPaths[key]?.remove(at: index)
                }
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

    private func finalizeCurrentPath(for pathDirectory: inout [String: [Path]]) {
        if !liveDrawingPath.isEmpty {
            pathDirectory[key, default: []].append(liveDrawingPath)
            liveDrawingPath = Path()
        }
    }
}

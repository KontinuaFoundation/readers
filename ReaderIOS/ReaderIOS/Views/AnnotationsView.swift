import PDFKit
import SwiftUI

struct AnnotationsView: View {
    @Binding var pagePaths: [String: [Path]]
    @Binding var highlightPaths: [String: [Path]]
    var key: String
    @Binding var selectedScribbleTool: String
    var nextPage: (() -> Void)?
    var previousPage: (() -> Void)?
    @State private var liveDrawingPath: Path = .init()

    var body: some View {
        Canvas { context, _ in
            if let paths = pagePaths[key] {
                for path in paths {
                    context.stroke(Path(path.cgPath), with: .color(.black), lineWidth: 2)
                }
            }
            if let hPaths = highlightPaths[key] {
                for path in hPaths {
                    context.stroke(Path(path.cgPath), with: .color(.yellow.opacity(0.5)), lineWidth: 5)
                }
            }
            context.stroke(Path(liveDrawingPath.cgPath),
                           with: selectedScribbleTool == "Highlight" ? .color(.blue.opacity(0.5)) : .color(.blue),
                           lineWidth: selectedScribbleTool == "Highlight" ? 5 : 2)
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
        if let pagePathsForCurrentPage = pagePaths[key] {
            for (index, path) in pagePathsForCurrentPage.enumerated() where path.contains(location) {
                pagePaths[key]?.remove(at: index)
                break
            }
        }
        if let highlightPathsForCurrentPage = highlightPaths[key] {
            for (index, path) in highlightPathsForCurrentPage.enumerated() where path.contains(location) {
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

    private func finalizeCurrentPath(for pathDirectory: inout [String: [Path]]) {
        if !liveDrawingPath.isEmpty {
            pathDirectory[key, default: []].append(liveDrawingPath)
            liveDrawingPath = Path()
        }
    }
}

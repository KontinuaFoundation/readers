import PDFKit
import SwiftUI

struct AnnotationsView: View {
    @Binding var pagePaths: [String: [Path]]
    @Binding var highlightPaths: [String: [Path]]
    var key: String
    @Binding var selectedScribbleTool: String
   
    @Binding var textAnnotations: [String: [TextAnnotation]] //textannoration object
    @State private var currentTextAnnotation: TextAnnotation? = nil // Track the currently edited text annotation
    @FocusState private var isTextFieldFocused: Bool // Corrected @FocusState

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
        
        // Draw text annotations
                    if let textAnns = textAnnotations[key] {
                        for textAnn in textAnns {
                            let text = SwiftUI.Text(textAnn.text) // Use SwiftUI.Text for context.drawText
                            let resolvedText = context.resolve(text)
                            let rect = CGRect(origin: textAnn.position, size: CGSize(width: 200, height: 100))
                            context.draw(resolvedText, in: rect) // Use draw, not drawText
                        }
                    }
                    // Draw live drawing path
                    context.stroke(Path(liveDrawingPath.cgPath),
                                   with: selectedScribbleTool == "Highlight" ? .color(.blue.opacity(0.5)) : .color(.blue),
                                   lineWidth: selectedScribbleTool == "Highlight" ? 5 : 2)
                }
                .overlay(
                    // Conditionally show text field over text annotation
                    Group {
                        if let currentTextAnnotation = currentTextAnnotation, selectedScribbleTool == "Text" {
                            TextAnnotationView(annotation: $currentTextAnnotation,
                                               isFocused: _isTextFieldFocused,
                                               commitText: commitTextAnnotation)
                                .position(x: currentTextAnnotation.position.x + 100, y: currentTextAnnotation.position.y + 50) // Adjust positioning as needed
                        }
                    }
                )
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
        .onTapGesture { location in
                    // Handle text annotation creation or selection
                    if selectedScribbleTool == "Text" {
                        if let existingAnnotationIndex = textAnnotations[key]?.firstIndex(where: { $0.position.distance(to: location) < 20 }) {
                            // Select existing annotation
                            currentTextAnnotation = textAnnotations[key]?[existingAnnotationIndex]
                            isTextFieldFocused = true
                        } else {
                            // Create a new text annotation
                            let newAnnotation = TextAnnotation(id: UUID().uuidString, text: "", position: location)
                            textAnnotations[key, default: []].append(newAnnotation)
                            currentTextAnnotation = newAnnotation
                            isTextFieldFocused = true
                        }
                    } else {
                        // Deselect text annotation if another tool is selected
                        currentTextAnnotation = nil
                        isTextFieldFocused = false
                    }
                }
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
    private func commitTextAnnotation() {
           if let currentTextAnnotation = currentTextAnnotation {
               if let index = textAnnotations[key]?.firstIndex(where: { $0.id == currentTextAnnotation.id }) {
                   textAnnotations[key]?[index] = currentTextAnnotation
               }
           }
           self.currentTextAnnotation = nil
           isTextFieldFocused = false
       }
}

struct TextAnnotation: Identifiable, Codable {
    let id: String
    var text: String
    var position: CGPoint
}

// TextAnnotationView struct
struct TextAnnotationView: View {
    @Binding var annotation: TextAnnotation?
    @FocusState var isFocused: Bool // Remove .Binding
    var commitText: () -> Void

    var body: some View {
        if let annotationBinding = Binding($annotation) {
            TextField("Enter text", text: annotationBinding.text)
                .focused($isFocused)
                .padding()
                .background(Color.white)
                .border(Color.gray)
                .frame(width: 200, height: 100)
                .onChange(of: isFocused) { focused in
                    if !focused {
                        commitText()
                    }
                }
        }
    }
}

// CGPoint Extension
extension CGPoint {
    func distance(to point: CGPoint) -> CGFloat {
        return sqrt(pow(point.x - x, 2) + pow(point.y - y, 2))
    }
}

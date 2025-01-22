import PDFKit
import SwiftUI

class AnnotationManager: ObservableObject {
    struct AnnotationData: Codable {
        let key: String
        let paths: [[CGPoint]]
        let colors: [CodableColor] // Array to store colors for each path
        let isHighlight: Bool
    }

    struct CodableColor: Codable, Equatable {
        var red: CGFloat
        var green: CGFloat
        var blue: CGFloat
        var alpha: CGFloat

        init(_ color: Color) {
            // Initialize the properties to default values first
            red = 0
            green = 0
            blue = 0
            alpha = 0

            // Now it's safe to use self in UIColor
            let uiColor = UIColor(color)
            uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        }

        var color: Color {
            Color(UIColor(red: red, green: green, blue: blue, alpha: alpha))
        }
    }
    
    // update this function to save new paths correctly
    func saveAnnotations(pagePaths: [String: [(path: Path, color: Color)]], highlightPaths: [String: [(path: Path, color: Color)]]) {
        var annotationData: [AnnotationData] = []

        // Serialize paths with colors
        for (key, pathsWithColors) in pagePaths {
            let pathPoints = pathsWithColors.map { $0.path.toPoints() }
            let colors = pathsWithColors.map { CodableColor($0.color) }
            annotationData.append(AnnotationData(key: key, paths: pathPoints, colors: colors, isHighlight: false))
        }

        // Serialize highlight paths with colors
        for (key, pathsWithColors) in highlightPaths {
            let pathPoints = pathsWithColors.map { $0.path.toPoints() }
            let colors = pathsWithColors.map { CodableColor($0.color) }
            annotationData.append(AnnotationData(key: key, paths: pathPoints, colors: colors, isHighlight: true))
        }

        // Convert to JSON
        do {
            let jsonData = try JSONEncoder().encode(annotationData)
            let url = getAnnotationsFileURL()
            try jsonData.write(to: url)
            print("Annotations saved to \(url)")
        } catch {
            print("Failed to save annotations: \(error)")
        }
    }

    private func getAnnotationsFileURL() -> URL {
        let fileManager = FileManager.default
        let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        return documentsURL.appendingPathComponent("annotations.json")
    }

    // update this function to load paths with colors correctly
    func loadAnnotations(pagePaths: inout [String: [(path: Path, color: Color)]], highlightPaths: inout [String: [(path: Path, color: Color)]]) {        let url = getAnnotationsFileURL()

        do {
            let data = try Data(contentsOf: url)
            let annotationData = try JSONDecoder().decode([AnnotationData].self, from: data)

            // Clear current paths
            pagePaths.removeAll()
            highlightPaths.removeAll()

            // Restore paths with colors
            for annotation in annotationData {
                let pathsWithColors = zip(annotation.paths, annotation.colors).map { (pathPoints, color) in
                    return (path: Path(points: pathPoints), color: color.color)
                }
                if annotation.isHighlight {
                    highlightPaths[annotation.key] = pathsWithColors
                } else {
                    pagePaths[annotation.key] = pathsWithColors
                }
            }
            print("Annotations loaded from \(url)")
        } catch {
            print("Failed to load annotations: \(error)")
        }
    }
}

extension Path {
    func toPoints() -> [CGPoint] {
        var points: [CGPoint] = []
        cgPath.applyWithBlock { element in
            let pointsPointer = element.pointee.points
            if element.pointee.type == .addLineToPoint || element.pointee.type == .moveToPoint {
                points.append(pointsPointer[0])
            }
        }
        return points
    }

    init(points: [CGPoint]) {
        self.init()
        guard let firstPoint = points.first else { return }
        move(to: firstPoint)
        for point in points.dropFirst() {
            addLine(to: point)
        }
    }
}

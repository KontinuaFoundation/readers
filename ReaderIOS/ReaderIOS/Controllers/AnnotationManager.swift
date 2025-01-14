//
//  AnnotationManager.swift
//  ReaderIOS
//
//  Created by Luis Rodriguez Jr. on 11/19/24.
//
import PDFKit
import SwiftUI

class AnnotationManager: ObservableObject {
    struct AnnotationData: Codable {
        let key: String
        let paths: [[CGPoint]]
        let isHighlight: Bool
    }

    func saveAnnotations(pagePaths: [String: [Path]], highlightPaths: [String: [Path]]) {
        var annotationData: [AnnotationData] = []

        // Serialize paths
        for (key, paths) in pagePaths {
            let pathPoints = paths.map { $0.toPoints() }
            annotationData.append(AnnotationData(key: key, paths: pathPoints, isHighlight: false))
        }

        // Serialize highlight paths
        for (key, paths) in highlightPaths {
            let pathPoints = paths.map { $0.toPoints() }
            annotationData.append(AnnotationData(key: key, paths: pathPoints, isHighlight: true))
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

    func loadAnnotations(pagePaths: inout [String: [Path]], highlightPaths: inout [String: [Path]]) {
        let url = getAnnotationsFileURL()

        do {
            let data = try Data(contentsOf: url)
            let annotationData = try JSONDecoder().decode([AnnotationData].self, from: data)

            // Clear current paths
            pagePaths.removeAll()
            highlightPaths.removeAll()

            // Restore paths
            for annotation in annotationData {
                let paths = annotation.paths.map { Path(points: $0) }
                if annotation.isHighlight {
                    highlightPaths[annotation.key] = paths
                } else {
                    pagePaths[annotation.key] = paths
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

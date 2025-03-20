import SwiftUI

class ZoomManager: ObservableObject {
    @Published private var zoomedIn: Bool = false
    @Published private var currentZoom: CGFloat = 0
    @Published private var totalZoom: CGFloat = 1.2
    @Published private var zoomPoint: UnitPoint = .center

    func zoomin() -> some Gesture {
        var magnification: some Gesture {
            MagnifyGesture()
                .onChanged { value in
                    self.currentZoom = value.magnification - 1
                    self.zoomedIn = true
                }
                .onEnded { _ in
                    self.totalZoom += self.currentZoom
                    self.currentZoom = 0
                    if self.totalZoom <= 1.2 {
                        self.zoomedIn = false
                    } else {
                        self.zoomedIn = true
                    }
                }
        }
        return magnification
    }

    func zoomout() -> some Gesture {
        var reset: some Gesture {
            TapGesture(count: 2)
                .onEnded { _ in
                    self.resetZoom()
                }
        }
        return reset
    }

    func newZoomPoint(newPoint: CGPoint, width: CGFloat, height: CGFloat) {
        if !zoomedIn {
            zoomPoint = UnitPoint(x: newPoint.x / width, y: newPoint.y / height)
        }
    }

    func newZoomLevel() -> CGFloat {
        max(min(totalZoom + currentZoom, 5.0), 1.2)
    }

    func resetZoom() {
        totalZoom = 1.2
        currentZoom = 0
        zoomedIn = false
        zoomPoint = .center
    }

    func getZoomedIn() -> Bool {
        zoomedIn
    }

    func getZoomPoint() -> UnitPoint {
        zoomPoint
    }

    func panZoomCenter(by translation: CGSize, in viewSize: CGSize) {
        // Calculate a speed multiplier based on current zoom level
        // The higher the zoom, the faster the panning
        let zoomLevel = newZoomLevel()
        let speedMultiplier = max(zoomLevel / 2.0, 1.0)
        
        // Normalize the translation relative to the view size with speed boost
        let deltaX = (translation.width / viewSize.width) * speedMultiplier
        let deltaY = (translation.height / viewSize.height) * speedMultiplier
        
        // Update the zoom point
        var newX = zoomPoint.x - deltaX
        var newY = zoomPoint.y - deltaY
        
        // Clamp the new values to [0, 1]
        newX = min(max(newX, 0), 1)
        newY = min(max(newY, 0), 1)
        
        zoomPoint = UnitPoint(x: newX, y: newY)
    }
}

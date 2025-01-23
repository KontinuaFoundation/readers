import SwiftUI

class ZoomManager: ObservableObject {
    @Published var zoomedIn: Bool = false
    @Published var currentZoom: CGFloat = 0
    @Published var totalZoom: CGFloat = 1.2
    @Published var zoomPoint: UnitPoint = .center

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
                    self.currentZoom = 0.0
                    self.totalZoom = 1.2
                    self.zoomedIn = false
                    self.zoomPoint = .center
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
}

import SwiftUI

class ZoomManager: ObservableObject {
    @Published var zoomedIn: Bool = false
    @Published var currentZoom: CGFloat = 0
    @Published var totalZoom: CGFloat = 1.2
    @Published var zoomPoint: CGPoint = .zero

    func zoomin() -> some Gesture {
        var magnification: some Gesture {
            MagnifyGesture()
                .onChanged { value in
                    self.currentZoom = value.magnification - 1
                    self.zoomedIn = true
                    value.startAnchor = (10,10)
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
                }
        }
        return reset
    }

    func newZoomLevel() -> CGFloat {
        totalZoom + currentZoom
    }

    func resetZoom() {
        totalZoom = 1.2
        currentZoom = 0
        zoomedIn = false
    }
}

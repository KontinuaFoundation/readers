//
//  ZoomableModifier.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import SwiftUI

struct ZoomableModifier: ViewModifier {
    @ObservedObject var zoomManager: ZoomManager
    var geometrySize: CGSize

    func body(content: Content) -> some View {
        content
                    .scaleEffect(zoomManager.newZoomLevel(), anchor: zoomManager.zoomedIn ? zoomManager.zoomPoint : .center)
                    .simultaneousGesture(zoomManager.zoomin())
                    .simultaneousGesture(zoomManager.zoomout())
                    .onTapGesture(count: 1, coordinateSpace: .local) { location in
                        zoomManager.newZoomPoint(newPoint: location, width: geometrySize.width, height: geometrySize.height)
                    }
    }
}

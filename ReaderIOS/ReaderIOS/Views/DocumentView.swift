import PDFKit
import SwiftUI

struct DocumentView: UIViewRepresentable {
    var pdfDocument: PDFDocument?
    // Binding directly connects the PDF components state with the parent, content view.
    @Binding var currentPageIndex: Int
    @Binding var resetZoom: Bool
    @Binding var zoomedIn: Bool

    func makeUIView(context: Context) -> PDFKit.PDFView {
        let pdfView = PDFKit.PDFView()
        configurePDFView(pdfView)

        let overlayView = UIView(frame: pdfView.bounds)
        overlayView.backgroundColor = .clear
        overlayView.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        // Add a pinch gesture recognizer to the overlay
        let pinchGesture = UIPinchGestureRecognizer(
            target: context.coordinator,
            action: #selector(context.coordinator.handlePinch(_:))
        )
        overlayView.addGestureRecognizer(pinchGesture)

        // Add double-tap gesture recognizer
        let doubleTapGesture = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(context.coordinator.handleDoubleTap(_:))
        )
        doubleTapGesture.numberOfTapsRequired = 2
        overlayView.addGestureRecognizer(doubleTapGesture)

        // Add the overlay on top of the PDFView
        pdfView.addSubview(overlayView)

        return pdfView
    }

    func updateUIView(_ uiView: PDFKit.PDFView, context _: Context) {
        guard let pdfDocument = pdfDocument else { return }

        if uiView.document != pdfDocument {
            uiView.document = pdfDocument
        }

        // Check if resetZoom is triggered
        if resetZoom {
            uiView.scaleFactor = uiView.scaleFactorForSizeToFit // Reset scale factor

            DispatchQueue.main.async {
                resetZoom = false // Reset the binding to avoid repeated resets
                zoomedIn = false
            }
        }

        goToPage(in: uiView)
    }

    private func configurePDFView(_ pdfView: PDFKit.PDFView) {
        pdfView.displayMode = .singlePage
        pdfView.displayDirection = .horizontal
        pdfView.document = pdfDocument
        pdfView.autoScales = true

        goToPage(in: pdfView)
    }

    private func goToPage(in pdfView: PDFKit.PDFView) {
        if let page = pdfDocument?.page(at: currentPageIndex) {
            pdfView.go(to: page)
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject {
        var parent: DocumentView

        init(_ parent: DocumentView) {
            self.parent = parent
        }

        @objc func handlePinch(_ sender: UIPinchGestureRecognizer) {
            guard let pdfView = sender.view?.superview as? PDFKit.PDFView else { return }
            let scale = sender.scale
            let newScaleFactor = pdfView.scaleFactor * scale

            // Constrain the scale factor within limits
            pdfView.scaleFactor = max(min(newScaleFactor, pdfView.maxScaleFactor), pdfView.minScaleFactor)

            parent.zoomedIn = !(pdfView.scaleFactor == pdfView.scaleFactorForSizeToFit)

            // Reset the gesture scale to avoid compounding the scale each time
            sender.scale = 1.0
        }

        @objc func handleDoubleTap(_ sender: UITapGestureRecognizer) {
            guard let pdfView = sender.view?.superview as? PDFKit.PDFView else { return }

            // Toggle zoom level between a zoomed-in scale and the default scale
            if pdfView.scaleFactor == pdfView.scaleFactorForSizeToFit {
                /*
                  disabling zoom in for now, needs location of tap detection
                  pdfView.scaleFactor = min(pdfView.maxScaleFactor, pdfView.scaleFactor * 2)  // Zoom in
                 parent.zoomedIn = true
                  */
            } else {
                pdfView.scaleFactor = pdfView.scaleFactorForSizeToFit // Reset zoom
                parent.zoomedIn = false
            }
        }
    }
}

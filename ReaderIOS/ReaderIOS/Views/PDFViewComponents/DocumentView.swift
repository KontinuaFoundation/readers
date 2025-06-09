import PDFKit
import SwiftUI

struct DocumentView: UIViewRepresentable {
    var pdfDocument: PDFDocument?
    // Binding directly connects the PDF components state with the parent, content view.
    @Binding var currentPageIndex: Int
    @Binding var pageFrame: CGRect

    func makeUIView(context _: Context) -> PDFKit.PDFView {
        let pdfView = PDFKit.PDFView()
        configurePDFView(pdfView)
        return pdfView
    }

    func updateUIView(_ uiView: PDFKit.PDFView, context _: Context) {
        guard let pdfDocument = pdfDocument else { return }

        if uiView.document != pdfDocument {
            uiView.document = pdfDocument
        }

        if let page = pdfDocument.page(at: currentPageIndex) {
            // Get the visible frame of the page in PDFView coordinates
            let convertedFrame = uiView.convert(page.bounds(for: .cropBox), from: page)
            DispatchQueue.main.async {
                pageFrame = convertedFrame
            }
            uiView.go(to: page)
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
    }
}

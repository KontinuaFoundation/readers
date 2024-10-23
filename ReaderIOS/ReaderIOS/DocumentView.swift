import SwiftUI
import PDFKit

struct DocumentView: UIViewRepresentable {
    var pdfDocument: PDFDocument?
    // Binding directly connects the PDF components state with the parent, content view.
    @Binding var currentPageIndex: Int

    func makeUIView(context: Context) -> PDFKit.PDFView {
        let pdfView = PDFKit.PDFView()
        configurePDFView(pdfView)
        return pdfView
    }

    func updateUIView(_ uiView: PDFKit.PDFView, context: Context) {
        guard let pdfDocument = pdfDocument else { return }
        if uiView.document != pdfDocument {
            uiView.document = pdfDocument
        }
        goToPage(in: uiView)
    }

    private func configurePDFView(_ pdfView: PDFKit.PDFView) {
        pdfView.autoScales = true
        pdfView.displayMode = .singlePage
        pdfView.displayDirection = .horizontal
        pdfView.document = pdfDocument
        goToPage(in: pdfView)
    }

    private func goToPage(in pdfView: PDFKit.PDFView) {
        if let page = pdfDocument?.page(at: currentPageIndex) {
            pdfView.go(to: page)
        }
    }
}


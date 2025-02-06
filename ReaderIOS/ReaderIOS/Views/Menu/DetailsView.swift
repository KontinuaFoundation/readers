//
//  DetailsView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import PDFKit
import SwiftUI

struct DetailView: View {
    @Binding var currentPdfFileName: String?
    @Binding var currentPage: Int
    @Binding var bookmarkLookup: [String: Set<Int>]
    @Binding var covers: [Cover]?
    @Binding var pdfDocument: PDFDocument?

    var body: some View {
        if currentPdfFileName != nil {
            PDFViewer(fileName: $currentPdfFileName, covers: $covers, currentPage: $currentPage)
        } else {
            ProgressView("Getting the latest workbook.")
        }
    }
}

//
//  PDFNetworking.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import Foundation
import PDFKit

public enum PDFNetworkingError: Error {
    case invalidURL
    case invalidData
}

public enum PDFNetworking {
    public static func fetchPDF(fileName: String) async throws -> PDFDocument {
        let baseURL = "http://localhost:8000/pdfs/"
        let urlString = baseURL + fileName

        guard let url = URL(string: urlString) else {
            throw PDFNetworkingError.invalidURL
        }

        let (data, _) = try await URLSession.shared.data(from: url)

        guard let document = PDFDocument(data: data) else {
            throw PDFNetworkingError.invalidData
        }

        return document
    }
}

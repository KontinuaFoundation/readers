//
//  NetworkingService.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import Foundation
import PDFKit.PDFDocument

enum NetworkError: Error {
    case invalidURL
    case noData
}

final class NetworkingService: ObservableObject {
    static let shared = NetworkingService()

    // Published property so views can observe loading state.
    @Published var isContentLoading: Bool = false
    // Internal counter to handle multiple concurrent requests.
    private var loadingCount = 0

    private init() {}

    // Configure a URLSession with no caching.
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.urlCache = nil
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: config)
    }()

    // MARK: - Loading State Helpers

    private func startLoading() {
        loadingCount += 1
        DispatchQueue.main.async {
            self.isContentLoading = true
        }
    }

    private func stopLoading() {
        loadingCount -= 1
        if loadingCount <= 0 {
            loadingCount = 0
            DispatchQueue.main.async {
                self.isContentLoading = false
            }
        }
    }

    // MARK: - Network Methods

    func fetchLatestCollection(completion: @escaping (Result<Collection, Error>) -> Void) {
        guard let url = URL(string: ApplicationConstants.API.baseURLString + ApplicationConstants.APIEndpoints
            .collections + "latest" + "?localization=en-US")
        else {
            completion(.failure(NetworkError.invalidURL))
            return
        }

        startLoading()
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let task = session.dataTask(with: request) { data, _, error in
            // Ensure stopLoading is called when this closure exits.
            defer { self.stopLoading() }

            if let error = error {
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }

            guard let data = data else {
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }

            do {
                let decoder = JSONDecoder()

                let collection = try decoder.decode(Collection.self, from: data)

                let latestCollection = collection

                DispatchQueue.main.async { completion(.success(latestCollection)) }
            } catch {
                DispatchQueue.main.async { completion(.failure(error)) }
            }
        }

        task.resume()
    }

    func fetchWorkbooks(collection: Collection, completion: @escaping (Result<[WorkbookPreview], Error>) -> Void) {
        guard let url = URL(string: ApplicationConstants.API.baseURLString + ApplicationConstants.APIEndpoints
            .collections + "\(collection.id)/")
        else {
            completion(.failure(NetworkError.invalidURL))
            return
        }
        startLoading()
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let task = session.dataTask(with: request) { data, _, error in
            defer { self.stopLoading() }

            if let error = error {
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }
            guard let data = data else {
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }
            do {
                let decoder = JSONDecoder()
                let detailedCollection = try decoder.decode(DetailedCollection.self, from: data)
                DispatchQueue.main.async { completion(.success(detailedCollection.workbooks)) }
            } catch {
                DispatchQueue.main.async { completion(.failure(error)) }
            }
        }
        task.resume()
    }

    func fetchWorkbook(id: Int, completion: @escaping (Result<Workbook, Error>) -> Void) {
        guard let url = URL(string: ApplicationConstants.API.baseURLString + ApplicationConstants.APIEndpoints
            .workbooks + "\(id)/")
        else {
            completion(.failure(NetworkError.invalidURL))
            return
        }
        startLoading()
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let task = session.dataTask(with: request) { data, _, error in
            defer { self.stopLoading() }

            if let error = error {
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }
            guard let data = data else {
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }
            do {
                let decoder = JSONDecoder()
                let workbook = try decoder.decode(Workbook.self, from: data)
                DispatchQueue.main.async { completion(.success(workbook)) }
            } catch {
                DispatchQueue.main.async { completion(.failure(error)) }
            }
        }
        task.resume()
    }

    func fetchPDF(workbook: Workbook, completion: @escaping (Result<PDFDocument, Error>) -> Void) {
        guard let url = URL(string: workbook.pdf) else {
            completion(.failure(NetworkError.invalidURL))
            return
        }

        print("Downloading the pdf from \(url)")
        startLoading()

        let task = session.dataTask(with: url) { data, _, error in
            defer { self.stopLoading() }

            if let error = error {
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }
            guard let data = data, let document = PDFDocument(data: data) else {
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }
            DispatchQueue.main.async { completion(.success(document)) }
        }
        task.resume()
    }
}

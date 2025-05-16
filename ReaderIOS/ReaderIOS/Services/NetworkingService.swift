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
            Logger.error("Invalid URL for latest collection", category: "Network")
            completion(.failure(NetworkError.invalidURL))
            return
        }
        Logger.Network.request(url.absoluteString)
        startLoading()
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let task = session.dataTask(with: request) { data, response, error in
            // Ensure stopLoading is called when this closure exits.
            defer { self.stopLoading() }

            if let error = error {
                Logger.Network.error(error, url: url.absoluteString)
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }

            guard let httpResponse = response as? HTTPURLResponse else {
                Logger.error("Invalid response type", category: "Network")
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }

            Logger.Network.response(url.absoluteString, statusCode: httpResponse.statusCode)

            guard let data = data else {
                Logger.error("No data received", category: "Network")
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }

            do {
                let decoder = JSONDecoder()

                let collection = try decoder.decode(Collection.self, from: data)

                let latestCollection = collection

                Logger.info("Successfully decoded collection: \(collection.id)", category: "Network")

                DispatchQueue.main.async { completion(.success(latestCollection)) }
            } catch {
                Logger.error("Failed to decode collection: \(error)", category: "Network")

                DispatchQueue.main.async { completion(.failure(error)) }
            }
        }

        task.resume()
    }

    func fetchWorkbooks(collection: Collection, completion: @escaping (Result<[WorkbookPreview], Error>) -> Void) {
        Logger.info("Fetching workbooks for collection: \(collection.id)", category: "Network")

        guard let url = URL(string: ApplicationConstants.API.baseURLString + ApplicationConstants.APIEndpoints
            .collections + "\(collection.id)/")
        else {
            Logger.error("Invalid URL for collection: \(collection.id)", category: "Network")
            completion(.failure(NetworkError.invalidURL))
            return
        }

        Logger.Network.request(url.absoluteString)
        startLoading()

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let task = session.dataTask(with: request) { data, response, error in
            defer { self.stopLoading() }

            if let error = error {
                Logger.Network.error(error, url: url.absoluteString)
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }

            guard let httpResponse = response as? HTTPURLResponse else {
                Logger.error("Invalid response type", category: "Network")
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }

            Logger.Network.response(url.absoluteString, statusCode: httpResponse.statusCode)

            guard let data = data else {
                Logger.error("No data received", category: "Network")
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }
            do {
                let decoder = JSONDecoder()
                let detailedCollection = try decoder.decode(DetailedCollection.self, from: data)
                Logger.info("Successfully decoded \(detailedCollection.workbooks.count) workbooks", category: "Network")
                DispatchQueue.main.async { completion(.success(detailedCollection.workbooks)) }
            } catch {
                Logger.error("Failed to decode workbooks: \(error)", category: "Network")
                DispatchQueue.main.async { completion(.failure(error)) }
            }
        }
        task.resume()
    }

    func fetchWorkbook(id: Int, completion: @escaping (Result<Workbook, Error>) -> Void) {
        Logger.info("Fetching workbook with ID: \(id)", category: "Network")

        guard let url = URL(string: ApplicationConstants.API.baseURLString + ApplicationConstants.APIEndpoints
            .workbooks + "\(id)/")
        else {
            Logger.error("Invalid URL for workbook: \(id)", category: "Network")
            completion(.failure(NetworkError.invalidURL))
            return
        }
        Logger.Network.request(url.absoluteString)
        startLoading()
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let task = session.dataTask(with: request) { data, response, error in
            defer { self.stopLoading() }

            if let error = error {
                Logger.Network.error(error, url: url.absoluteString)
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }
            guard let httpResponse = response as? HTTPURLResponse else {
                Logger.error("Invalid response type", category: "Network")
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }
            Logger.Network.response(url.absoluteString, statusCode: httpResponse.statusCode)

            guard let data = data else {
                Logger.error("No data received", category: "Network")
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }
            do {
                let decoder = JSONDecoder()
                let workbook = try decoder.decode(Workbook.self, from: data)
                Logger.info("Successfully decoded workbook: \(workbook.id)", category: "Network")
                DispatchQueue.main.async { completion(.success(workbook)) }
            } catch {
                Logger.error("Failed to decode workbook: \(error)", category: "Network")
                DispatchQueue.main.async { completion(.failure(error)) }
            }
        }
        task.resume()
    }

    func fetchPDF(workbook: Workbook, completion: @escaping (Result<PDFDocument, Error>) -> Void) {
        Logger.error("Invalid PDF URL for workbook: \(workbook.id)", category: "Network")
        guard let url = URL(string: workbook.pdf) else {
            completion(.failure(NetworkError.invalidURL))
            return
        }
        Logger.info("Downloading PDF for workbook: \(workbook.id)", category: "Network")
        Logger.Network.request(url.absoluteString)
        startLoading()

        let task = session.dataTask(with: url) { data, response, error in
            defer { self.stopLoading() }

            if let error = error {
                Logger.Network.error(error, url: url.absoluteString)
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }
            if let httpResponse = response as? HTTPURLResponse {
                Logger.Network.response(url.absoluteString, statusCode: httpResponse.statusCode)
            }
            guard let data = data, let document = PDFDocument(data: data) else {
                Logger.error("Failed to create PDF document from data", category: "Network")
                DispatchQueue.main.async { completion(.failure(NetworkError.noData)) }
                return
            }
            Logger.info("Successfully loaded PDF for workbook: \(workbook.id)", category: "Network")
            DispatchQueue.main.async { completion(.success(document)) }
        }
        task.resume()
    }
}

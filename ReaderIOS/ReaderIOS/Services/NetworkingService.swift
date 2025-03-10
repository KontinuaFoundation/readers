//
//  NetworkingService.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import Foundation
import PDFKit.PDFDocument

let APIURL = "http://18.189.208.93/api/"

enum NetworkError: Error {
    case invalidURL
    case noData
}


// 1.) /api/collections/?localization=en-US to get latest collection
// 2.) /api/collections/<id>/ to get workbooks for that collection
// 3.) Then we can fetch workbooks / pdfs as needed...



final class NetworkingService {
    static let shared = NetworkingService()

    private init() {}

    // Configure a URLSession with no caching
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.urlCache = nil
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: config)
    }()
    
    func fetchLatestCollection(completion: @escaping (Result<Collection, Error>) -> Void) {
        guard let url = URL(string: APIURL + "collections/?localization=en-US") else {
            completion(.failure(NetworkError.invalidURL))
            return
        }
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData
        
        let task = session.dataTask(with: request) { data, _, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            guard let data = data else {
                completion(.failure(NetworkError.noData))
                return
            }
            do {
                
                let decoder = JSONDecoder()
                let collections = try decoder.decode([Collection].self, from: data)
                
                if collections.isEmpty {
                    completion(.failure(NetworkError.noData))
                    return
                }
                
                let latestCollection = collections[0]
                
                
                DispatchQueue.main.async {
                    completion(.success(latestCollection))
                }
                
            } catch {
                completion(.failure(error))
            }
        }
        task.resume()
    }

    func fetchWorkbooks(collection: Collection ,completion: @escaping (Result<[WorkbookPreview], Error>) -> Void) {
        
        guard let url = URL(string: APIURL + "collections/\(collection.id)/") else {
            completion(.failure(NetworkError.invalidURL))
            return
        }
        
        
        

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData
        
        

        let task = session.dataTask(with: request) { data, _, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            guard let data = data else {
                completion(.failure(NetworkError.noData))
                return
            }
            do {
                let decoder = JSONDecoder()
                let detailedCollection = try decoder.decode(DetailedCollection.self, from: data)
                DispatchQueue.main.async {
                    completion(.success(detailedCollection.workbooks))
                }
            } catch {
                completion(.failure(error))
            }
        }
        task.resume()
    }

    func fetchWorkbook(id: Int, completion: @escaping (Result<Workbook, Error>) -> Void) {
        guard let url = URL(string: APIURL + "workbooks/\(id)/") else {
            completion(.failure(NetworkError.invalidURL))
            return
        }

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let task = session.dataTask(with: request) { data, _, error in
            
            if let error = error {
                completion(.failure(error))
                return
            }
            
            guard let data = data else {
                completion(.failure(NetworkError.noData))
                return
            }
            do {
                let decoder = JSONDecoder()
                let chapters = try decoder.decode(Workbook.self, from: data)
                DispatchQueue.main.async {
                    completion(.success(chapters))
                }
            } catch {
                completion(.failure(error))
            }
        }
        task.resume()
    }
    
    /// Fetches a PDF document given its file name.
    func fetchPDF(workbook: Workbook, completion: @escaping (Result<PDFDocument, Error>) -> Void) {
        
        guard let url = URL(string: workbook.pdf) else {
            completion(.failure(NetworkError.invalidURL))
            return
        }
        
        print("Downloading the pdf from \(url)")

        let task = session.dataTask(with: url) { data, _, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            guard let data = data, let document = PDFDocument(data: data) else {
                completion(.failure(NetworkError.noData))
                return
            }
            DispatchQueue.main.async {
                completion(.success(document))
            }
        }
        task.resume()
    }
}

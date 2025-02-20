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

    func fetchWorkbooks(completion: @escaping (Result<[Workbook], Error>) -> Void) {
        guard let url = URL(string: "http://localhost:8000/workbooks.json") else {
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
                let workbooks = try decoder.decode([Workbook].self, from: data)
                DispatchQueue.main.async {
                    completion(.success(workbooks))
                }
            } catch {
                completion(.failure(error))
            }
        }
        task.resume()
    }

    func fetchChapters(metaName: String, completion: @escaping (Result<[Chapter], Error>) -> Void) {
        guard let url = URL(string: "http://localhost:8000/meta/\(metaName)") else {
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
                let chapters = try decoder.decode([Chapter].self, from: data)
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
      func fetchPDF(fileName: String, completion: @escaping (Result<PDFDocument, Error>) -> Void) {
          let baseURL = "http://localhost:8000/pdfs/"
          let urlString = baseURL + fileName
          guard let url = URL(string: urlString) else {
              completion(.failure(NetworkError.invalidURL))
              return
          }
          
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

//
//  FeedbackManager.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 3/12/25.
//

import Combine
import SwiftUI

/// Model for feedback data to be submitted to the API
struct FeedbackSubmission: Encodable {
    let userEmail: String
    let description: String
    let workbookID: Int?
    let chapterNumber: Int
    let pageNumber: Int
    let majorVersion: Int
    let minorVersion: Int
    let localization: String

    enum CodingKeys: String, CodingKey {
        case userEmail = "user_email"
        case description
        case workbookID = "workbook_id"
        case chapterNumber = "chapter_number"
        case pageNumber = "page_number"
        case majorVersion = "major_version"
        case minorVersion = "minor_version"
        case localization
    }
}

/// Defines possible errors when submitting feedback
enum FeedbackError: Error, LocalizedError {
    case emptyEmail
    case invalidEmail
    case emptyDescription
    case invalidURL
    case dataPreparationError
    case networkError(Error)
    case serverError(Int, String?)
    case parseError(Error)

    var errorDescription: String? {
        switch self {
        case .emptyEmail:
            "Email is required"
        case .invalidEmail:
            "Please enter a valid email address"
        case .emptyDescription:
            "Feedback description is required"
        case .invalidURL:
            "Invalid URL configuration"
        case .dataPreparationError:
            "Error preparing feedback data"
        case let .networkError(error):
            "Network error: \(error.localizedDescription)"
        case let .serverError(code, message):
            message ?? "Server error (code: \(code))"
        case let .parseError(error):
            "Error parsing server response: \(error.localizedDescription)"
        }
    }
}

class FeedbackManager: ObservableObject {
    // MARK: - Published Properties

    /// Controls visibility of the feedback sheet
    @Published var isShowingFeedback = false

    // MARK: - Context Properties

    /// Current page being viewed
    var currentPage: Int = 0

    /// Current workbook being viewed
    var currentWorkbook: Workbook?

    /// Collection metadata
    var collection: Collection?

    // MARK: - Private Properties

    /// Manager for chapter operations
    private var chapterManager: ChapterManager?

    /// Source for initialization data if needed
    private let initializationManager: InitializationManager

    // MARK: - Initializers

    init(initializationManager: InitializationManager = InitializationManager()) {
        self.initializationManager = initializationManager
    }

    // MARK: - Public Methods

    /// Shows the feedback view
    func showFeedback() {
        isShowingFeedback = true
    }

    /// Sets the current workbook and initializes the chapter manager
    func setWorkbook(_ workbook: Workbook) {
        currentWorkbook = workbook
        chapterManager = ChapterManager(chapters: workbook.chapters)
    }

    /// Submits user feedback to the API
    /// - Parameters:
    ///   - email: User's email address
    ///   - feedbackBody: Feedback content
    ///   - completion: Callback with result
    func submitFeedback(
        email: String,
        feedbackBody: String,
        completion: @escaping (Result<Void, FeedbackError>) -> Void
    ) {
        // Validate inputs
        do {
            try validateFeedbackInput(email: email, description: feedbackBody)
        } catch let error as FeedbackError {
            completion(.failure(error))
            return
        } catch {
            completion(.failure(.dataPreparationError))
            return
        }

        // Log collection info for debugging
        logCollectionInfo()

        // Prepare submission data
        let collectionValues = getRequiredCollectionValues()
        let chapterNumber = getCurrentChapterNumber()

        let submission = FeedbackSubmission(
            userEmail: email,
            description: feedbackBody,
            workbookID: currentWorkbook?.id,
            chapterNumber: chapterNumber,
            pageNumber: currentPage + 1, // Convert from 0-based index
            majorVersion: collectionValues.majorVersion,
            minorVersion: collectionValues.minorVersion,
            localization: collectionValues.localization
        )

        // Submit to API
        submitToAPI(submission: submission) { result in
            switch result {
            case .success:
                completion(.success(()))
            case let .failure(error):
                completion(.failure(error))
            }
        }
    }

    // MARK: - Private Methods

    /// Logs the current collection information to the console
    private func logCollectionInfo() {
        if let collection = collection {
            print(
                "Collection available: ID=\(collection.id)," +
                    "majorVersion=\(collection.majorVersion)," +
                    "minorVersion=\(collection.minorVersion)," +
                    "localization=\(collection.localization)"
            )
        } else {
            print("Collection is nil")
        }
    }

    /// Validates email address format using a regular expression
    private func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }

    /// Validates feedback input data
    private func validateFeedbackInput(email: String, description: String) throws {
        guard !email.isEmpty else {
            throw FeedbackError.emptyEmail
        }

        guard !description.isEmpty else {
            throw FeedbackError.emptyDescription
        }

        guard isValidEmail(email) else {
            throw FeedbackError.invalidEmail
        }
    }

    /// Gets the current chapter number based on the page
    private func getCurrentChapterNumber() -> Int {
        // Default chapter number
        let defaultChapterNumber = 1

        guard let workbook = currentWorkbook else {
            return defaultChapterNumber
        }

        return determineCurrentChapter(workbook: workbook, page: currentPage)?.chapNum ?? defaultChapterNumber
    }

    /// Determines which chapter contains the given page
    private func determineCurrentChapter(workbook: Workbook, page: Int) -> Chapter? {
        if chapterManager == nil {
            chapterManager = ChapterManager(chapters: workbook.chapters)
        }

        return chapterManager?.getChapter(forPage: page)
    }

    /// Gets the required collection metadata values for feedback submission
    private func getRequiredCollectionValues() -> (majorVersion: Int, minorVersion: Int, localization: String) {
        // First check if we have a collection in our FeedbackManager
        if let collection = collection {
            print(
                "Using collection values from FeedbackManager: " +
                    "majorVersion=\(collection.majorVersion)," +
                    "minorVersion=\(collection.minorVersion)," +
                    "localization=\(collection.localization)"
            )
            return (collection.majorVersion, collection.minorVersion, collection.localization)
        }

        // Try the injected InitializationManager
        if let latestCollection = initializationManager.latestCollection {
            print(
                "Using latestCollection from InitializationManager:" +
                    "majorVersion=\(latestCollection.majorVersion), " +
                    "minorVersion=\(latestCollection.minorVersion)," +
                    "localization=\(latestCollection.localization)"
            )
            return (latestCollection.majorVersion, latestCollection.minorVersion, latestCollection.localization)
        }

        // Default values as last resort
        print("No collection found, using default values")
        return (1, 0, "en-US")
    }

    /// Submits feedback data to the API
    private func submitToAPI(
        submission: FeedbackSubmission,
        completion: @escaping (Result<Void, FeedbackError>) -> Void
    ) {
        // Prepare the request
        guard let url = URL(string:
            ApplicationConstants.API.baseURLString +
                ApplicationConstants.APIEndpoints.feedback)
        else {
            completion(.failure(.invalidURL))
            return
        }

        // Prepare the JSON data
        guard let jsonData = try? JSONEncoder().encode(submission) else {
            completion(.failure(.dataPreparationError))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = jsonData

        // Send the request
        URLSession.shared.dataTask(with: request) { data, response, error in
            // Handle the response on the main thread
            DispatchQueue.main.async {
                if let error = error {
                    completion(.failure(.networkError(error)))
                    return
                }

                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.serverError(0, "Invalid server response")))
                    return
                }

                if httpResponse.statusCode == 201 {
                    completion(.success(()))
                } else {
                    let errorMessage = self.parseErrorResponse(from: data, statusCode: httpResponse.statusCode)
                    completion(.failure(.serverError(httpResponse.statusCode, errorMessage)))
                }
            }
        }.resume()
    }

    /// Extracts error message from server response
    private func parseErrorResponse(from data: Data?, statusCode: Int) -> String? {
        guard let data = data else {
            return "Error submitting feedback. Status: \(statusCode)"
        }

        do {
            // Try to parse as JSON first
            if let errorResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                // Look for common error message fields
                return errorResponse["message"] as? String
                    ?? errorResponse["error"] as? String
                    ?? errorResponse["detail"] as? String
                    ?? "Unknown error from server"
            } else {
                // If not JSON, try as plain text
                return String(data: data, encoding: .utf8)
            }
        } catch {
            return "Error parsing server response: \(error.localizedDescription)"
        }
    }
}

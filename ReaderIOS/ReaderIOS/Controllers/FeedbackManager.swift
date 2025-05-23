//
//  FeedbackManager.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 3/12/25.
//

import Combine
import SwiftUI

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
        includeLogs: Bool = true,
        completion: @escaping (Result<Void, FeedbackError>) -> Void
    ) {
        Logger.info("Attempting to submit feedback", category: "Feedback")
        Logger.UserAction.featureUsed("Feedback Submission")

        // Validate inputs
        do {
            try validateFeedbackInput(email: email, description: feedbackBody)
        } catch let error as FeedbackError {
            Logger.error("Feedback validation failed: \(error.localizedDescription)", category: "Feedback")
            completion(.failure(error))
            return
        } catch {
            Logger.error("Unexpected validation error: \(error)", category: "Feedback")
            completion(.failure(.dataPreparationError))
            return
        }

        // Get logs if requested (default is true)
        let applicationLogs: [String: Any]?

        if includeLogs {
            // Get the JSON logs directly - don't convert to string
            applicationLogs = Logger.getLogsForFeedbackJSON()
            Logger.info("Including structured logs in feedback", category: "Feedback")
        } else {
            applicationLogs = nil
            Logger.info("Submitting feedback without logs", category: "Feedback")
        }

        // Prepare submission data
        let collectionValues = getRequiredCollectionValues()
        let chapterNumber = getCurrentChapterNumber()

        // Create the submission data as a dictionary first
        var submissionData: [String: Any] = [
            "user_email": email,
            "description": feedbackBody,
            "workbook_id": currentWorkbook?.id ?? NSNull(),
            "chapter_number": chapterNumber,
            "page_number": currentPage + 1, // Convert from 0-based index
            "major_version": collectionValues.majorVersion,
            "minor_version": collectionValues.minorVersion,
            "localization": collectionValues.localization
        ]

        // Add logs if available
        if let logs = applicationLogs {
            submissionData["logs"] = logs
        }

        // Log submission details (without sensitive data)
        Logger.info(
            "Submitting feedback for workbook: \(currentWorkbook?.id ?? -1), page: \(currentPage + 1)",
            category: "Feedback"
        )

        // Submit to API
        submitToAPI(submissionData: submissionData) { result in
            switch result {
            case .success:
                Logger.info("Feedback submitted successfully", category: "Feedback")
                completion(.success(()))
            case let .failure(error):
                Logger.error("Feedback submission failed: \(error.localizedDescription)", category: "Feedback")
                completion(.failure(error))
            }
        }
    }

    // MARK: - Private Methods

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
            Logger.info("Using Default Chapter Number", category: "Feedback")
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
            return (collection.majorVersion, collection.minorVersion, collection.localization)
        }

        // Try the injected InitializationManager
        if let latestCollection = initializationManager.latestCollection {
            return (latestCollection.majorVersion, latestCollection.minorVersion, latestCollection.localization)
        }

        // Default values as last resort
        Logger.warning("No collection found, using default values", category: "Feedback")
        return (1, 0, "en-US")
    }

    /// Submits feedback data to the API
    private func submitToAPI(
        submissionData: [String: Any],
        completion: @escaping (Result<Void, FeedbackError>) -> Void
    ) {
        // Prepare the request
        guard let url = URL(string:
            ApplicationConstants.API.baseURLString +
                ApplicationConstants.APIEndpoints.feedback)
        else {
            Logger.error("Failed to create URL for feedback API call", category: "Feedback")
            completion(.failure(.invalidURL))
            return
        }

        // Prepare the JSON data
        guard let jsonData = try? JSONSerialization.data(withJSONObject: submissionData, options: []) else {
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

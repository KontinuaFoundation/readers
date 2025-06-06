//
//  FeedbackManager.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 3/12/25.
//

import Combine
import SwiftUI

class FeedbackManager: ObservableObject {
    @Published var isShowingFeedback = false

    var currentPage: Int = 0
    var currentWorkbook: Workbook?
    var collection: Collection?

    private var chapterManager: ChapterManager?
    private let initializationManager: InitializationManager

    init(initializationManager: InitializationManager = InitializationManager()) {
        self.initializationManager = initializationManager
    }

    func showFeedback() {
        isShowingFeedback = true
    }

    func setWorkbook(_ workbook: Workbook) {
        currentWorkbook = workbook
        chapterManager = ChapterManager(chapters: workbook.chapters)
    }

    func submitFeedback(
        email: String,
        feedbackBody: String,
        includeLogs: Bool = true,
        completion: @escaping (Result<Void, FeedbackError>) -> Void
    ) {
        Logger.info("Attempting to submit feedback", category: "Feedback")
        Logger.UserAction.featureUsed("Feedback Submission")

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

        let submissionData = prepareSubmissionData(
            email: email,
            feedbackBody: feedbackBody,
            includeLogs: includeLogs
        )

        Logger.info(
            "Submitting feedback for workbook: \(currentWorkbook?.id ?? -1), page: \(currentPage + 1)",
            category: "Feedback"
        )

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

    func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }

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

    private func prepareSubmissionData(
        email: String,
        feedbackBody: String,
        includeLogs: Bool
    ) -> [String: Any] {
        let applicationLogs: [String: Any]?

        if includeLogs {
            applicationLogs = Logger.getLogsForFeedbackJSON()
            Logger.info("Including structured logs in feedback", category: "Feedback")
        } else {
            applicationLogs = nil
            Logger.info("Submitting feedback without logs", category: "Feedback")
        }

        let collectionValues = getRequiredCollectionValues()
        let chapterNumber = getCurrentChapterNumber()

        var submissionData: [String: Any] = [
            "user_email": email,
            "description": feedbackBody,
            "chapter_number": chapterNumber,
            "page_number": currentPage + 1,
            "major_version": collectionValues.majorVersion,
            "minor_version": collectionValues.minorVersion,
            "localization": collectionValues.localization
        ]

        if let workbookId = currentWorkbook?.id {
            submissionData["workbook_id"] = workbookId
        }
        submissionData["workbook_id"] = currentWorkbook?.id ?? NSNull()

        if let logs = applicationLogs {
            submissionData["logs"] = logs
        }

        return submissionData
    }

    private func getCurrentChapterNumber() -> Int {
        let defaultChapterNumber = 1

        guard let workbook = currentWorkbook else {
            Logger.info("Using Default Chapter Number", category: "Feedback")
            return defaultChapterNumber
        }

        return determineCurrentChapter(workbook: workbook, page: currentPage)?.chapNum ?? defaultChapterNumber
    }

    private func determineCurrentChapter(workbook: Workbook, page: Int) -> Chapter? {
        if chapterManager == nil {
            chapterManager = ChapterManager(chapters: workbook.chapters)
        }
        return chapterManager?.getChapter(forPage: page)
    }

    private func getRequiredCollectionValues() -> (majorVersion: Int, minorVersion: Int, localization: String) {
        if let collection = collection {
            return (collection.majorVersion, collection.minorVersion, collection.localization)
        }

        if let latestCollection = initializationManager.latestCollection {
            return (latestCollection.majorVersion, latestCollection.minorVersion, latestCollection.localization)
        }

        Logger.warning("No collection found, using default values", category: "Feedback")
        return (1, 0, "en_US")
    }

    private func submitToAPI(
        submissionData: [String: Any],
        completion: @escaping (Result<Void, FeedbackError>) -> Void
    ) {
        guard let url = URL(string:
            ApplicationConstants.API.baseURLString +
                ApplicationConstants.APIEndpoints.feedback)
        else {
            Logger.error("Failed to create URL for feedback API call", category: "Feedback")
            completion(.failure(.invalidURL))
            return
        }

        Logger.info("Submitting to URL: \(url.absoluteString)", category: "Feedback")

        guard let jsonData = try? JSONSerialization.data(withJSONObject: submissionData, options: []) else {
            completion(.failure(.dataPreparationError))
            return
        }

        if let jsonString = String(data: jsonData, encoding: .utf8) {
            Logger.info("Request JSON: \(jsonString)", category: "Feedback")
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = jsonData

        Logger.info("Request headers: \(request.allHTTPHeaderFields ?? [:])", category: "Feedback")

        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    completion(.failure(.networkError(error)))
                    return
                }

                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(.serverError(0, "Invalid server response")))
                    return
                }

                Logger.info("Response headers: \(httpResponse.allHeaderFields)", category: "Feedback")

                if httpResponse.statusCode == 201 {
                    completion(.success(()))
                } else {
                    let parseResult = self.parseErrorResponse(from: data, statusCode: httpResponse.statusCode)

                    if parseResult.isParseError {
                        completion(.failure(.parseError(NSError(
                            domain: "FeedbackParsingError",
                            code: httpResponse.statusCode,
                            userInfo: [NSLocalizedDescriptionKey: parseResult.message ?? "Unknown parsing error"]
                        ))))
                    } else {
                        completion(.failure(.serverError(httpResponse.statusCode, parseResult.message)))
                    }
                }
            }
        }.resume()
    }

    private func parseErrorResponse(from data: Data?, statusCode: Int) -> (message: String?, isParseError: Bool) {
        guard let data = data else {
            return ("Error submitting feedback. Status: \(statusCode)", false)
        }

        if let rawString = String(data: data, encoding: .utf8) {
            Logger.debug("Raw server response (\(statusCode)): \(rawString)", category: "Feedback")
        }

        do {
            if let errorResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                let message = errorResponse["message"] as? String
                    ?? errorResponse["error"] as? String
                    ?? errorResponse["detail"] as? String
                    ?? errorResponse["errors"] as? String
                    ?? "Unknown error from server"

                return (message, false)
            } else {
                Logger.warning("Server response is not JSON (\(statusCode))", category: "Feedback")
                let textMessage = String(data: data, encoding: .utf8) ?? "Unknown server error"
                return ("Server error: \(textMessage)", false)
            }
        } catch {
            Logger.error("Failed to parse server response: \(error.localizedDescription)", category: "Feedback")
            let fallbackMessage = String(data: data, encoding: .utf8) ?? "Unknown parsing error"
            return ("Error parsing server response: \(fallbackMessage)", true)
        }
    }
}

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

    // Store the context needed for feedback
    var currentWorkbook: Workbook?
    var currentPage: Int = 0
    var collection: Collection?

    // Show the feedback view
    func showFeedback() {
        isShowingFeedback = true
    }

    func submitFeedback(email: String, description: String, completion: @escaping (Bool, String?) -> Void) {
        print("FeedbackManager - Collection object before submission:")
        if let collection = collection {
            print(
                "  Collection available: ID=\(collection.id), majorVersion=\(collection.majorVersion), minorVersion=\(collection.minorVersion), localization=\(collection.localization)"
            )
        } else {
            print("  Collection is nil")
        }

        guard !email.isEmpty else {
            completion(false, "Email is required")
            return
        }

        guard !description.isEmpty else {
            completion(false, "Feedback description is required")
            return
        }

        // Email format validation
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        guard emailPredicate.evaluate(with: email) else {
            completion(false, "Please enter a valid email address")
            return
        }

        // Prepare the feedback data
        var feedbackData: [String: Any] = [
            "user_email": email,
            "description": description
        ]

        // Add required workbook info
        if let workbook = currentWorkbook {
            feedbackData["workbook_id"] = workbook.id
        }

        // Always include chapter_number
        var chapterNumber = 1 // Default
        if let workbook = currentWorkbook {
            if let chapter = determineCurrentChapter(workbook: workbook, page: currentPage) {
                chapterNumber = chapter.chapNum
            }
        }
        feedbackData["chapter_number"] = chapterNumber

        // Add page number
        feedbackData["page_number"] = currentPage + 1 // Convert from 0-based index

        // Add collection info
        let collectionValues = getRequiredCollectionValues()
        feedbackData["major_version"] = collectionValues.majorVersion
        feedbackData["minor_version"] = collectionValues.minorVersion
        feedbackData["localization"] = collectionValues.localization

        // Submit to API
        submitToAPI(data: feedbackData, completion: completion)
    }

    // Submit feedback to the API
    private func submitToAPI(data: [String: Any], completion: @escaping (Bool, String?) -> Void) {
        print("Submitting feedback with data: \(data)")
        // Prepare the request
        guard let url = URL(string: ApplicationConstants.API.baseURLString +
            ApplicationConstants.APIEndpoints.feedback)
        else {
            completion(false, "Invalid URL configuration")
            return
        }

        // Prepare the JSON data
        guard let jsonData = try? JSONSerialization.data(withJSONObject: data) else {
            completion(false, "Error preparing feedback data")
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = jsonData

        // Send the request
        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("Network error: \(error.localizedDescription)")
                    completion(false, "Error: \(error.localizedDescription)")
                    return
                }

                // Check the response
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(false, "Invalid server response")
                    return
                }

                // Log the full response for debugging
                print("Server response status code: \(httpResponse.statusCode)")

                // Try to extract and print response data regardless of status code
                if let data = data, let responseString = String(data: data, encoding: .utf8) {
                    print("Server response body: \(responseString)")
                }

                // Check the status code
                if httpResponse.statusCode == 200 {
                    completion(true, nil)
                } else {
                    // Attempt to parse error message from response
                    if let data = data {
                        do {
                            if let errorResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                                // Try different common error message fields
                                let message = errorResponse["message"] as? String
                                    ?? errorResponse["error"] as? String
                                    ?? errorResponse["detail"] as? String
                                    ?? "Unknown error from server"
                                completion(false, message)
                            } else {
                                // If not JSON, try to extract as plain text
                                let errorText = String(data: data, encoding: .utf8) ?? "Error submitting feedback"
                                completion(false, errorText)
                            }
                        } catch {
                            completion(false, "Error parsing server response: \(error.localizedDescription)")
                        }
                    } else {
                        completion(false, "Error submitting feedback. Status: \(httpResponse.statusCode)")
                    }
                }
            }
        }.resume()
    }

    // Determine the current chapter based on the current page
    private func determineCurrentChapter(workbook: Workbook, page: Int) -> Chapter? {
        workbook.chapters.first { chapter in
            let chapterIndex = workbook.chapters.firstIndex(where: { $0.id == chapter.id })!
            let nextChapterIndex = chapterIndex + 1
            let nextChapterStartPage = nextChapterIndex < workbook.chapters.count ?
                workbook.chapters[nextChapterIndex].startPage : Int.max

            return page >= chapter.startPage - 1 && page < nextChapterStartPage
        }
    }

    func getRequiredCollectionValues() -> (majorVersion: Int, minorVersion: Int, localization: String) {
        // First check if we have a collection in our FeedbackManager
        if let collection = collection {
            print(
                "Using collection values from FeedbackManager: majorVersion=\(collection.majorVersion), minorVersion=\(collection.minorVersion), localization=\(collection.localization)"
            )
            return (collection.majorVersion, collection.minorVersion, collection.localization)
        }

        // Try to create a new InitializationManager to access its properties
        let initManager = InitializationManager()
        if let latestCollection = initManager.latestCollection {
            print(
                "Using latestCollection from InitializationManager:" + 
                "majorVersion=\(latestCollection.majorVersion), " +
                "minorVersion=\(latestCollection.minorVersion)," + 
                "localization=\(latestCollection.localization)"
            )
            return (latestCollection.majorVersion, latestCollection.minorVersion, latestCollection.localization)
        }

        // As a last resort, use default values
        print("No collection found, using default values")
        return (1, 0, "en-US")
    }
}

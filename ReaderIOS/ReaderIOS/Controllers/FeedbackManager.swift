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

    func showFeedback() {
        isShowingFeedback = true
    }

    func submitFeedback(email: String, description: String, completion: @escaping (Bool, String?) -> Void) {
        // Prepare the feedback data
        var feedbackData: [String: Any] = [
            "email": email,
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
        if let collection = collection {
            feedbackData["major_version"] = collection.majorVersion
            feedbackData["minor_version"] = collection.minorVersion
            feedbackData["localization"] = collection.localization
        }

        // Submit to API
        submitToAPI(data: feedbackData, completion: completion)
    }

    private func determineCurrentChapter(workbook: Workbook, page: Int) -> Chapter? {
        workbook.chapters.first { chapter in
            let chapterIndex = workbook.chapters.firstIndex(where: { $0.id == chapter.id })!
            let nextChapterIndex = chapterIndex + 1
            let nextChapterStartPage = nextChapterIndex < workbook.chapters.count ?
                workbook.chapters[nextChapterIndex].startPage : Int.max

            return page >= chapter.startPage - 1 && page < nextChapterStartPage
        }
    }

    private func submitToAPI(data: [String: Any], completion: @escaping (Bool, String?) -> Void) {
        guard let url = URL(string: ApplicationConstants.API.baseURLString +
            ApplicationConstants.APIEndpoints.feedback)
        else {
            completion(false, "Invalid URL configuration")
            return
        }

        guard let jsonData = try? JSONSerialization.data(withJSONObject: data) else {
            completion(false, "Error preparing feedback data")
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = jsonData

        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    completion(false, "Error: \(error.localizedDescription)")
                    return
                }

                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(false, "Invalid server response")
                    return
                }

                if httpResponse.statusCode == 200 {
                    completion(true, nil)
                } else {
                    if let data = data,
                       let errorResponse = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let message = errorResponse["message"] as? String
                    {
                        completion(false, message)
                    } else {
                        completion(false, "Error submitting feedback. Status: \(httpResponse.statusCode)")
                    }
                }
            }
        }.resume()
    }
}

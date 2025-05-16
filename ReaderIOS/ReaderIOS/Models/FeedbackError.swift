//
//  FeedbackError.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 5/13/25.
//
import Foundation

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

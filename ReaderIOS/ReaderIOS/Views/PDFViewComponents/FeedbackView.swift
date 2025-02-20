//
//  FeedbackView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 11/13/24.
//

import SwiftUI

struct FeedbackView: View {
    @Environment(\.dismiss) var dismiss
    @State private var email: String = ""
    @State private var feedback: String = ""
    @State private var isSubmitting = false
    @State private var showError = false
    @State private var errorMessage = ""
    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                    TextEditor(text: $feedback)
                        .frame(height: 200)
                }

                Button(action: submitFeedback) {
                    HStack {
                        Text(isSubmitting ? "Submitting..." : "Submit Feedback")
                        if isSubmitting {
                            ProgressView()
                        }
                    }
                }
                .disabled(isSubmitting || email.isEmpty || feedback.isEmpty)
            }
            .navigationTitle("Feedback")
            .navigationBarItems(trailing: Button("Cancel") { dismiss() })
            .alert("Error", isPresented: $showError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage)
            }
        }
    }

    private func submitFeedback() {
        isSubmitting = true

        guard let url = URL(string: "http://localhost:8000/mentapp/api/feedback/") else {
            showError(message: "Invalid URL configuration")
            return
        }

        let feedbackData = [
            "email": email,
            "feedback": feedback
        ]

        guard let jsonData = try? JSONSerialization.data(withJSONObject: feedbackData) else {
            showError(message: "Error preparing feedback data")
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = jsonData

        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                isSubmitting = false

                if let error = error {
                    showError(message: "Error submitting feedback: \(error.localizedDescription)")
                    return
                }

                guard let httpResponse = response as? HTTPURLResponse else {
                    showError(message: "Invalid server response")
                    return
                }

                if httpResponse.statusCode == 200 {
                    dismiss()
                } else {
                    if let data = data,
                       let errorResponse = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let message = errorResponse["message"] as? String
                    {
                        showError(message: message)
                    } else {
                        showError(message: "Error submitting feedback. Status: \(httpResponse.statusCode)")
                    }
                }
            }
        }.resume()
    }

    private func showError(message: String) {
        errorMessage = message
        showError = true
        isSubmitting = false
    }
}

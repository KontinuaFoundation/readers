//
//  FeedbackView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 11/13/24.
//

import SwiftUI

struct FeedbackButton: View {
    /**
     Displays a button that triggers the feedback view.
     - Parameters:
         - feedbackManager: The feedback manager to handle the feedback view.
         - workbook: The workbook to provide context for the feedback.
         - currentPage: The current page number to provide context for the feedback.
         - collection: The collection to provide context for the feedback.
     - Returns: A button that triggers the feedback view.
     - Note: The feedback view will be displayed with the provided context.
     */
    @ObservedObject var feedbackManager: FeedbackManager
    var workbook: Workbook?
    var currentPage: Int
    var collection: Collection?

    var body: some View {
        Button(action: {
            // Debug logging
            print("FeedbackButton - Setting context:")
            if let workbook = workbook {
                print("  Passing workbook ID: \(workbook.id)")
            } else {
                print("  No workbook to pass")
            }

            print("  Passing page: \(currentPage)")

            // Try to get collection from multiple sources
            var effectiveCollection = collection
            if effectiveCollection == nil {
                // Try to get from InitializationManager
                let initManager = InitializationManager()
                effectiveCollection = initManager.latestCollection
                print("  Using collection from InitializationManager: \(effectiveCollection?.id ?? -1)")
            }

            if let effectiveCollection = effectiveCollection {
                print("  Using collection: ID=\(effectiveCollection.id)")
            } else {
                print("  No collection available from any source")
            }

            // Update current context
            feedbackManager.currentWorkbook = workbook
            feedbackManager.currentPage = currentPage
            feedbackManager.collection = effectiveCollection // Use the effective collection

            // Show feedback
            feedbackManager.showFeedback()
        }, label: {
            Image(systemName: "message.fill")
                .font(.system(size: 20))
                .foregroundColor(.white)
                .padding(8)
                .background(Color.blue)
                .clipShape(Circle())
                .shadow(radius: 2)
        })
    }
}

struct FeedbackView: View {
    /**
     Displays a form for submitting feedback.
     - Parameters:
         - feedbackManager: The feedback manager to handle the feedback submission.
     - Returns: A form for submitting feedback.
     - Note: The feedback view allows the user to submit feedback with an email address and description.
     */
    @ObservedObject var feedbackManager: FeedbackManager
    @Environment(\.dismiss) var dismiss
    @State private var email: String = ""
    @State private var feedback: String = ""
    @State private var isSubmitting = false
    @State private var showSuccessAlert = false
    @State private var showErrorAlert = false
    @State private var errorMessage = ""
    @FocusState private var isEmailFocused: Bool
    @FocusState private var isFeedbackFocused: Bool

    // Button factory method
    static func button(
        feedbackManager: FeedbackManager,
        workbook: Workbook?,
        currentPage: Int,
        collection: Collection?
    ) -> some View {
        FeedbackButton(
            feedbackManager: feedbackManager,
            workbook: workbook,
            currentPage: currentPage,
            collection: collection
        )
    }

    // Form view
    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .focused($isEmailFocused)

                    TextEditor(text: $feedback)
                        .frame(height: 200)
                        .focused($isFeedbackFocused)
                }

                Button(
                    action: {
                        // Dismiss keyboard first, then submit
                        isEmailFocused = false
                        isFeedbackFocused = false
                        submitFeedback()
                    },
                    label: {
                        HStack {
                            Text(isSubmitting ? "Submitting..." : "Submit Feedback")
                            if isSubmitting {
                                ProgressView()
                            }
                        }
                    }
                )
                .disabled(isSubmitting || email.isEmpty || feedback.isEmpty)
            }
            .navigationTitle("Feedback")
            .navigationBarItems(trailing: Button("Cancel") {
                feedbackManager.isShowingFeedback = false
            })
            // Success alert
            .alert("Success", isPresented: $showSuccessAlert) {
                Button("OK") {
                    // Manually close the sheet by setting the manager's state
                    feedbackManager.isShowingFeedback = false
                }
            } message: {
                Text("Your feedback has been submitted. Thank you!")
            }
            // Error alert
            .alert("Error", isPresented: $showErrorAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage)
            }
            .toolbar {
                ToolbarItem(placement: .keyboard) {
                    Button("Done") {
                        isEmailFocused = false
                        isFeedbackFocused = false
                    }
                }
            }
        }
    }

    private func submitFeedback() {
        /**
         - Submits the feedback with the current email and description.
         - Note: This method will trigger the feedback submission and handle the result.
          */
        isSubmitting = true
        // Submit feedback to the API via the feedback manager instance
        feedbackManager.submitFeedback(email: email, feedbackBody: feedback) { result in
            // Handle the result on the main queue
            DispatchQueue.main.async {
                isSubmitting = false
                switch result {
                case .success:
                    showSuccessAlert = true
                case let .failure(error):
                    errorMessage = error.localizedDescription
                    showErrorAlert = true
                }
            }
        }
    }
}

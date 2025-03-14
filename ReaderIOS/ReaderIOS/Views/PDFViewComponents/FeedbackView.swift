//
//  FeedbackView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 11/13/24.
//

import SwiftUI

struct FeedbackButton: View {
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
    @ObservedObject var feedbackManager: FeedbackManager
    @Environment(\.dismiss) var dismiss
    @State private var email: String = ""
    @State private var feedback: String = ""
    @State private var isSubmitting = false
    @State private var showError = false
    @State private var errorMessage = ""

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

        feedbackManager.submitFeedback(email: email, description: feedback) { success, errorMsg in
            isSubmitting = false

            if success {
                dismiss()
            } else if let message = errorMsg {
                errorMessage = message
                showError = true
            }
        }
    }
}

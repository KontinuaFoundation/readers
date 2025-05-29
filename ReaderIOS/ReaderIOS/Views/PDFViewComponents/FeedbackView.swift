import SwiftUI

struct FeedbackButton: View {
    @ObservedObject var feedbackManager: FeedbackManager
    var workbook: Workbook?
    var currentPage: Int
    var collection: Collection?

    var body: some View {
        Button(action: {
            feedbackManager.currentWorkbook = workbook
            feedbackManager.currentPage = currentPage

            let effectiveCollection = collection ?? InitializationManager().latestCollection
            feedbackManager.collection = effectiveCollection

            feedbackManager.showFeedback()
        }, label: {
            Text("Submit Feedback")
                .foregroundColor(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color.blue)
                .cornerRadius(12)
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
    @State private var showSuccessAlert = false
    @State private var showErrorAlert = false
    @State private var errorMessage = ""
    @State private var showEmailError = false

    @FocusState private var isEmailFocused: Bool
    @FocusState private var isFeedbackFocused: Bool

    private let emailDefaultsKey = "savedUserEmail"

    private var isEmailValid: Bool {
        feedbackManager.isValidEmail(email)
    }

    private var canSubmit: Bool {
        !isSubmitting &&
            isEmailValid &&
            !email.trimmingCharacters(in: .whitespaces).isEmpty &&
            !feedback.trimmingCharacters(in: .whitespaces).isEmpty
    }

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
                // MARK: - Email Section

                Section(header: Text("Your Email")) {
                    VStack(alignment: .leading, spacing: 6) {
                        TextField("Email", text: $email)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                            .autocorrectionDisabled(true)
                            .disableAutocorrection(true)
                            .padding(10)
                            .background(Color.white)
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(showEmailError ? Color.red : Color.gray.opacity(0.4), lineWidth: 1)
                            )
                            .focused($isEmailFocused)
                            .onChange(of: email) { _, newValue in
                                showEmailError = !isEmailValid && !newValue.isEmpty
                                UserDefaults.standard.setValue(newValue, forKey: emailDefaultsKey)
                            }

                        if showEmailError {
                            Text("Invalid email")
                                .font(.caption)
                                .foregroundColor(.gray)
                                .padding(.leading, 4)
                        }
                    }
                    .listRowBackground(Color(UIColor.systemGroupedBackground))
                }

                // MARK: - Feedback Section

                Section(header: Text("Your Feedback")) {
                    ZStack(alignment: .topLeading) {
                        TextEditor(text: $feedback)
                            .frame(height: 200)
                            .padding(10)
                            .background(Color.white)
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color.gray.opacity(0.4), lineWidth: 1)
                            )
                            .focused($isFeedbackFocused)
                    }
                    .listRowBackground(Color(UIColor.systemGroupedBackground))
                }

                // MARK: - Submit Button

                Section {
                    HStack {
                        Spacer()
                        Button(
                            action: {
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
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(canSubmit ? Color.blue : Color.gray)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                            }
                        )
                        .disabled(!canSubmit)
                        Spacer()
                    }
                    .listRowBackground(Color(UIColor.systemGroupedBackground))
                }
            }
            .navigationTitle("Feedback")
            .navigationBarItems(trailing: Button("Cancel") {
                feedbackManager.isShowingFeedback = false
            })
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .onAppear {
                if let savedEmail = UserDefaults.standard.string(forKey: emailDefaultsKey) {
                    email = savedEmail
                }
            }
            .alert("Success", isPresented: $showSuccessAlert) {
                Button("OK") {
                    feedbackManager.isShowingFeedback = false
                }
            } message: {
                Text("Your feedback has been submitted. Thank you!")
            }
            .alert("Error", isPresented: $showErrorAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage)
            }
        }
    }

    private func submitFeedback() {
        isSubmitting = true
        feedbackManager.submitFeedback(email: email, feedbackBody: feedback) { result in
            DispatchQueue.main.async {
                isSubmitting = false
                switch result {
                case .success:
                    showSuccessAlert = true
                    showErrorAlert = false
                case let .failure(error):
                    errorMessage = error.localizedDescription
                    showErrorAlert = true
                    showSuccessAlert = false
                    print("Feedback submission failed: \(error.localizedDescription)")
                }
            }
        }
    }
}

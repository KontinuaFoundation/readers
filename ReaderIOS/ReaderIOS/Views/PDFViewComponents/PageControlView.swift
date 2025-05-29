//
//  PageControlView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/4/25.
//
import SwiftUI

struct PageControlView: View {
    // Binding to the external currentPage variable.
    @Binding var currentPage: Int
    // Total number of pages available.
    let totalPages: Int

    @State private var textFieldValue: String = ""

    @FocusState private var isTextFieldFocused: Bool

    var body: some View {
        ZStack {
            Color.clear
                .contentShape(Rectangle())
                .onTapGesture {
                    if isTextFieldFocused {
                        isTextFieldFocused = false
                    }
                }

            HStack(spacing: 8) {
                Button(action: decrementPage) {
                    Image(systemName: "chevron.left")
                        .font(.title)
                        .frame(width: 10, height: 24)
                }
                .disabled(currentPage == 0)

                TextField("", text: $textFieldValue, onEditingChanged: { isEditing in
                    if isEditing {
                        // Clear the text when the user starts editing.
                        DispatchQueue.main.async {
                            textFieldValue = ""
                        }
                    }
                }, onCommit: commitTextField)
                    .multilineTextAlignment(.center)
                    .frame(width: 50)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .keyboardType(.numberPad)
                    // change keyboard hide to "go" button
                    .submitLabel(.go)
                    .focused($isTextFieldFocused)
                    // When focus is lost (for example, user taps outside), commit the text.
                    .onChange(of: isTextFieldFocused) { focused in
                        if !focused {
                            commitTextField()
                        }
                    }

                Text("/ \(totalPages)")

                Button(action: incrementPage) {
                    Image(systemName: "chevron.right")
                        .font(.title)
                        .frame(width: 24, height: 24)
                }
                .disabled(currentPage >= totalPages - 1)
            }
        }
        .onAppear {
            textFieldValue = "\(currentPage + 1)"
        }
        // When the external currentPage changes, update the text field to match.
        .onChange(of: currentPage) { newValue in
            textFieldValue = "\(newValue + 1)"
        }
    }

    private func incrementPage() {
        if currentPage < totalPages - 1 {
            currentPage += 1
        }
    }

    private func decrementPage() {
        if currentPage > 0 {
            currentPage -= 1
        }
    }

    /// Called when the user commits an edit in the text field.
    private func commitTextField() {
        // Try to convert the entered text to an integer.
        if let newPage = Int(textFieldValue), newPage >= 1, newPage <= totalPages {
            currentPage = newPage - 1
        } else {
            // If the input is invalid, reset the text field to the current page.
            print("invalid page number")
            DispatchQueue.main.async {
                textFieldValue = "\(currentPage + 1)"
            }
        }
    }
}

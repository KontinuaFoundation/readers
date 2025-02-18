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

    // A state variable to hold the text field's value.
    @State private var textFieldValue: String = ""
    // A FocusState variable to track whether the text field is focused.
    @FocusState private var isTextFieldFocused: Bool

    var body: some View {
        // Wrap everything in a ZStack with a clear background to catch taps.
        ZStack {
            // This invisible background registers taps that dismiss the keyboard.
            Color.clear
                .contentShape(Rectangle())
                .onTapGesture {
                    if isTextFieldFocused {
                        isTextFieldFocused = false
                    }
                }

            HStack(spacing: 8) {
                // Left arrow: Decrement the current page.
                Button(action: decrementPage) {
                    Image(systemName: "chevron.left")
                        .font(.title)
                        .frame(width: 10, height: 24)
                }
                .disabled(currentPage == 0) // Disable if already on the first page.

                // Center text field: Display and edit the page number.
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
                    // Bind the text field’s focus to the FocusState variable.
                    .focused($isTextFieldFocused)
                    // When focus is lost (for example, user taps outside), commit the text.
                    .onChange(of: isTextFieldFocused) { focused in
                        if !focused {
                            commitTextField()
                        }
                    }

                // Right arrow: Increment the current page.
                Button(action: incrementPage) {
                    Image(systemName: "chevron.right")
                        .font(.title)
                        .frame(width: 24, height: 24)
                }
                .disabled(currentPage >= totalPages - 1) // Disable if already on the last page.
            }
        }
        // When the view appears, initialize the text field with the current page.
        .onAppear {
            textFieldValue = "\(currentPage + 1)"
        }
        // When the external currentPage changes, update the text field to match.
        .onChange(of: currentPage) { newValue in
            textFieldValue = "\(newValue + 1)"
        }
    }

    /// Increments the current page if possible.
    private func incrementPage() {
        if currentPage < totalPages - 1 {
            currentPage += 1
        }
    }

    /// Decrements the current page if possible.
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

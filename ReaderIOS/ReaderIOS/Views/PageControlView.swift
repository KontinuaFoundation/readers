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

    var body: some View {
        HStack {
            // Left arrow: Decrement the current page.
            Button(action: decrementPage) {
                Image(systemName: "chevron.left")
                    .font(.title)
            }
            .disabled(currentPage == 0) // Disable if already on the first page.

            // Center text field: Display and edit the page number.
            TextField("", text: $textFieldValue, onCommit: commitTextField)
                .multilineTextAlignment(.center)
                .frame(width: 50)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .keyboardType(.numberPad)
                .padding(.horizontal)

            // Right arrow: Increment the current page.
            Button(action: incrementPage) {
                Image(systemName: "chevron.right")
                    .font(.title)
            }
            .disabled(currentPage >= totalPages - 1) // Disable if already on the last page.
        }
        // When the view appears, initialize the text field with the current page.
        .onAppear {
            self.textFieldValue = "\(self.currentPage + 1)"
        }
        // When the external currentPage changes (for example, via the arrows),
        // update the text field to match.
        .onChange(of: currentPage) { newValue in
            self.textFieldValue = "\(newValue + 1)"
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
                self.textFieldValue = "\(self.currentPage + 1)"
            }
        }
    }
}

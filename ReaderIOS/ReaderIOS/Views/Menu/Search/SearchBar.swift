//
//  SearchBar.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import SwiftUI

struct SearchBar: View {
    @Binding var text: String
    var onClear: (() -> Void)?

    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            TextField("Search chapters and words", text: $text)
                .textFieldStyle(PlainTextFieldStyle())
                .disableAutocorrection(true)
            if !text.isEmpty {
                Button(action: {
                    text = ""
                    onClear?()
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(8)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

//
//  ClearScreenView.swift
//  ReaderIOS
//
//  Created by Molly Sandler on 12/10/24.
//
import SwiftUI

struct ClearScreenView: View {
    @Environment(\.dismiss) var dismiss
    var onConfirm: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Text("Clear Screen")
                .font(.title)
                .fontWeight(.bold)

            Text("Are you sure you want to clear all markup on this page? This action cannot be undone.")
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            HStack(spacing: 20) {
                Button("Cancel") {
                    dismiss()
                }
                .buttonStyle(.bordered)

                Button("Clear") {
                    onConfirm()
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
                .tint(.red)
            }
        }
        .padding()
    }
}

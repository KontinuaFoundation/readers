//
//  SplashView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import SwiftUI

struct SplashView: View {
    var loadFailed: Bool = false

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 20) {
                // Logo image. Replace "logo" with the name of your image asset.
                Image("kontinua-logo-full")
                    .resizable()
                    .scaledToFit()
                    .frame(width: geometry.size.width * 0.7)
                    .accessibilityLabel("App Logo")

                if loadFailed {
                    // Display red alert symbol and red error text.
                    Image(systemName: "exclamationmark.triangle.fill")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 50, height: 50)
                        .foregroundColor(.red)

                    Text("Failed to load workbooks.\nPlease try again.")
                        .multilineTextAlignment(.center)
                        .font(.title3)
                        .foregroundColor(.red)
                } else {
                    // Show progress bar and loading text.
                    ProgressView()
                        .controlSize(.large)
                        .padding(.horizontal)

                    Text("Loading workbooks...")
                        .font(.title3)
                        .foregroundColor(.secondary)
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .padding()
        }
    }
}

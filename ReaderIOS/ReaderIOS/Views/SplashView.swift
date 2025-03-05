//
//  SplashView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import SwiftUI

struct SplashView: View {
    @ObservedObject var initManager: InitializationManager

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color(UIColor.systemBackground)
                    .edgesIgnoringSafeArea(.all)

                // Logo positioned above the halfway point.
                Image("kontinua-logo-full")
                    .resizable()
                    .scaledToFit()
                    .frame(width: geometry.size.width * 0.7)
                    .accessibilityLabel("App Logo")
                    .position(
                        x: geometry.size.width / 2,
                        y: geometry.size.height * 0.40
                    )

                // Loading or error content positioned below the halfway point.
                Group {
                    if initManager.loadFailed {
                        VStack(spacing: 20) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 50, height: 50)
                                .foregroundColor(.red)

                            Text("Failed to load workbooks.\n Please check your internet connection and try again.")
                                .multilineTextAlignment(.center)
                                .font(.body)
                                .foregroundColor(.red)
                                .frame(width: geometry.size.width * 0.7)
                                .fixedSize(horizontal: false, vertical: true)

                            if initManager.attempts < FetchConstants.maxAttempts {
                                Button("Reload") {
                                    // Hide error and call reload with the desired delay.
                                    initManager.loadFailed = false
                                    initManager
                                        .loadInitialData(delay: FetchConstants.attemptDelay * initManager.attempts)
                                }
                                .font(.body)
                                .buttonStyle(.bordered)
                            }
                        }
                    } else {
                        VStack(spacing: 20) {
                            ProgressView()
                                .controlSize(.large)
                                .padding(.horizontal)

                            Text("Loading workbooks...")
                                .font(.body)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .position(
                    x: geometry.size.width / 2,
                    y: geometry.size.height * 0.60
                )
            }
        }
    }
}

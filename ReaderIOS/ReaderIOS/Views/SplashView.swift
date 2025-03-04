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
            VStack(spacing: 20) {
                // Logo image. Replace "logo" with the name of your image asset.
                Image("kontinua-logo-full")
                    .resizable()
                    .scaledToFit()
                    .frame(width: geometry.size.width * 0.7)
                    .accessibilityLabel("App Logo")

                if initManager.loadFailed {
                    // Display red alert symbol and red error text.
                    Image(systemName: "exclamationmark.triangle.fill")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 50, height: 50)
                        .foregroundColor(.red)

                    Text("Failed to load workbooks.\n Please check your internet connection and try again.")
                        .multilineTextAlignment(.center)
                        .font(.title3)
                        .foregroundColor(.red)

                    if(initManager.attempts < FetchConstants.maxAttempts){
                        Button("Reload", action: {
                            initManager.loadFailed = false
                            initManager.loadInitialData(delay: FetchConstants.attemptDelay * initManager.attempts)
                        })
                        .font(.title3)
                        .buttonStyle(.bordered)
                    }
                    
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

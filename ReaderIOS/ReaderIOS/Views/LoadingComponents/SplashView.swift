//
//  SplashView.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/19/25.
//

import SwiftUI

struct SplashView: View {
    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 20) {
                // Logo image. Replace "logo" with the name of your image asset.
                Image("kontinua-logo-full")
                    .resizable()
                    .scaledToFit()
                    .frame(width: geometry.size.width * 0.7)
                    .accessibilityLabel("App Logo")

                // Progress bar
                ProgressView()
                    .controlSize(.large)
                    .padding(.horizontal)

                // Optional loading text
                Text("Loading workbooks...")
                    .font(.title3)
                    .foregroundColor(.secondary)
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .padding()
        }
    }
}

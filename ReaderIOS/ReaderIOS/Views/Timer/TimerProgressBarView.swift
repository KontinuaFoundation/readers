//
//  TimerProgressBarView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import SwiftUI

struct TimerProgressBarView: View {
    @ObservedObject var timerManager: TimerManager
    @Binding var showingFeedback: Bool

    var body: some View {
        HStack(spacing: 0) {
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    if timerManager.isTimerRunning || timerManager.isPaused {
                        Rectangle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(width: geometry.size.width, height: 4)
                    }

                    Rectangle()
                        .fill(timerManager.isPaused ? Color
                            .yellow : (timerManager.progress >= 1 ? Color.green : Color.red))
                        .frame(
                            width: geometry.size.width * CGFloat(timerManager.progress),
                            height: 4
                        )
                        .animation(.linear(duration: 0.1), value: timerManager.progress)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: 4)

            Button {
                showingFeedback = true
            } label: {
                Image(systemName: "message.fill")
                    .font(.system(size: 16))
                    .foregroundColor(.white)
                    .padding(8)
                    .background(Color.blue)
                    .clipShape(Circle())
                    .shadow(radius: 2)
            }
        }
        .padding(.leading, 25)
    }
}

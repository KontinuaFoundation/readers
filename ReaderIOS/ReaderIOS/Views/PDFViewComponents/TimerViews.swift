//
//  TimerViews.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/6/25.
//
import SwiftUI

// MARK: - Timer Controls View

struct TimerControlsView: View {
    @ObservedObject var timerManager: TimerManager

    var body: some View {
        // Timer Controls
        if timerManager.isTimerRunning {
            Button {
                timerManager.pauseTimer()
            } label: {
                Image(systemName: "pause.circle")
                    .foregroundColor(.yellow)
            }

            Button {
                timerManager.restartTimer()
            } label: {
                Image(systemName: "arrow.clockwise.circle")
                    .foregroundColor(.blue)
            }

            Button {
                timerManager.cancelTimer()
            } label: {
                Image(systemName: "xmark.circle")
                    .foregroundColor(.red)
            }
        } else if timerManager.isPaused {
            Button {
                timerManager.unpauseTimer()
            } label: {
                Image(systemName: "play.circle")
                    .foregroundColor(.green)
            }

            Button {
                timerManager.restartTimer()
            } label: {
                Image(systemName: "arrow.clockwise.circle")
                    .foregroundColor(.blue)
            }

            Button {
                timerManager.cancelTimer()
            } label: {
                Image(systemName: "xmark.circle")
                    .foregroundColor(.red)
            }
        } else {
            Menu {
                Button("15 Minutes") {
                    timerManager.startTimer(duration: 15 * 60)
                }
                Button("20 Minutes") {
                    timerManager.startTimer(duration: 20 * 60)
                }
                Button("25 Minutes") {
                    timerManager.startTimer(duration: 25 * 60)
                }
            } label: {
                Text("Timer")
                    .padding(5)
                    .foregroundColor(.blue)
                    .cornerRadius(8)
            }
        }
    }
}

// MARK: - Timer Progress View

struct TimerProgressView: View {
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

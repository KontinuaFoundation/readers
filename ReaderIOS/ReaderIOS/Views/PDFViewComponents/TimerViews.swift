//
//  TimerViews.swift
//  ReaderIOS
//
//  Created by Ethan Handelman on 2/6/25.
//
import SwiftUI

// MARK: - Timer Constants

enum TimerConstants {
    // use these constants to set Timer menu options
    static let options: [Int] = [15, 20, 25]

    // must be a multiple of customStep
    static let defaultCustomOption: Double = 35
    static let customStep: Double = 5
    static let customMin: Double = 5
    static let customMax: Double = 60

    // DO NOT MODIFY: Calculated constants
    static let actualMin: Double = customMin - customStep
    static let actualMax: Double = customMax + customStep
}

// MARK: - Timer Controls View

struct TimerControlsView: View {
    @ObservedObject var timerManager: TimerManager
    @State private var customMinutes: Double = TimerConstants.defaultCustomOption

    var body: some View {
        // Timer Controls
        Menu {
            if timerManager.isTimerRunning || timerManager.isPaused {
                let elapsed = timerManager.selectedDuration - timerManager.remainingDuration
                Text("\(formatTime(elapsed)) / \(formatTime(timerManager.selectedDuration))")
                    .foregroundColor(.gray)
            }

            // creates Menu options based off options in TimerConstants
            ForEach(TimerConstants.options, id: \.self) { minutes in
                Button("\(minutes) Minutes") {
                    timerManager.startTimer(duration: TimeInterval(minutes * 60))
                }
            }

            // Custom option with a slider below it
            VStack(alignment: .leading) {
                Button("\(Int(customMinutes)) Minutes") {
                    timerManager.startTimer(duration: TimeInterval(Int(customMinutes) * 60))
                }

                Slider(
                    value: $customMinutes,
                    in: TimerConstants.actualMin ... TimerConstants.actualMax,
                    step: TimerConstants.customStep
                )
                .padding(.horizontal)
                .onChange(of: customMinutes) {
                    // logic to loop counter
                    if customMinutes == TimerConstants.actualMax {
                        customMinutes = TimerConstants.customMin
                    } else if customMinutes == TimerConstants.actualMin {
                        customMinutes = TimerConstants.customMax
                    }
                }
            }
        } label: {
            Text("Timer")
                .padding(5)
                .foregroundColor(.blue)
                .cornerRadius(8)
        }
    }

    func formatTime(_ seconds: TimeInterval) -> String {
        let minutes = Int(seconds) / 60
        let seconds = Int(seconds) % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

// MARK: - Timer Progress View

struct TimerProgressView: View {
    @ObservedObject var timerManager: TimerManager
    @Binding var showingFeedback: Bool

    var body: some View {
        HStack(spacing: 0) {
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
            }

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
                        .animation(.linear(duration: 0.01), value: timerManager.progress)
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

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
    @State private var previousCustomMinutes: Double = TimerConstants.defaultCustomOption
    @State private var deltaSymbol: String = ""

    var body: some View {
        Menu {
            if timerManager.isTimerRunning || timerManager.isPaused {
                let elapsed = timerManager.selectedDuration - timerManager.remainingDuration
                Text("\(formatTime(elapsed)) / \(formatTime(timerManager.selectedDuration))")
                    .foregroundColor(.gray)
            }

            ForEach(TimerConstants.options, id: \.self) { minutes in
                Button("\(minutes) Minutes") {
                    timerManager.startTimer(duration: TimeInterval(minutes * 60))
                }
            }

            // Custom option with slider and delta text animation
            VStack(alignment: .leading) {
                ZStack {
                    Button(action: {
                        timerManager.startTimer(duration: TimeInterval(Int(customMinutes) * 60))
                    }, label: {
                        if deltaSymbol != "" {
                            Label("\(Int(customMinutes)) Minutes", systemImage: deltaSymbol)
                        } else {
                            Label("\(Int(customMinutes)) Minutes", systemImage: "1.circle")
                                .labelStyle(.titleOnly)
                        }

                    })
                }

                Slider(
                    value: $customMinutes,
                    in: TimerConstants.actualMin ... TimerConstants.actualMax,
                    step: TimerConstants.customStep
                )
                .padding(.horizontal)
                .onChange(of: customMinutes) { newValue in
                    let delta = newValue - previousCustomMinutes
                    if abs(delta) == TimerConstants.customMax {
                        deltaSymbol = "clock.arrow.trianglehead.2.counterclockwise.rotate.90"
                    } else {
                        deltaSymbol = delta > 0 ? "plus.circle" : "minus.circle"
                    }
                    // Hide the delta text after 0.75 seconds
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.75) {
                        deltaSymbol = ""
                    }

                    previousCustomMinutes = newValue

                    // Looping logic for the slider
                    if newValue == TimerConstants.actualMax {
                        customMinutes = TimerConstants.customMin
                    } else if newValue == TimerConstants.actualMin {
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
        }
        .padding(.leading, 25)
    }
}

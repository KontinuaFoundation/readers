//
//  TimerMenuView.swift
//  ReaderIOS
//
//  Created by Jonas Schiessl on 2/5/25.
//

import SwiftUI

struct TimerMenuView: View {
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
                Button("Clear Timer") {
                    timerManager.cancelTimer()
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

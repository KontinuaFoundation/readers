//
//  TimerManager.swift
//  ReaderIOS
//
//  Created by Molly Sandler on 11/12/24.
//

import SwiftUI

class TimerManager: ObservableObject {
    @Published var selectedDuration: TimeInterval = 0
    @Published var progress: Double = 0
    @Published var isTimerRunning: Bool = false
    @Published var remainingDuration: TimeInterval = 0
    @Published var isPaused: Bool = false

    private var timer: Timer?

    func startTimer(duration: TimeInterval) {
        selectedDuration = duration
        progress = 0
        remainingDuration = duration
        timer?.invalidate()
        isTimerRunning = true
        isPaused = false

        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            self.progress += 1 / duration
            self.remainingDuration -= 1
            if self.remainingDuration <= 0 {
                self.timer?.invalidate()
                self.timer = nil
                self.isTimerRunning = false
                self.progress = 1
            }
        }
    }

    func pauseTimer() {
        timer?.invalidate()
        timer = nil
        isTimerRunning = false
        isPaused = true
    }

    func unpauseTimer() {
        guard !isTimerRunning else { return }

        isTimerRunning = true
        isPaused = false

        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            self.progress += 1 / self.selectedDuration
            self.remainingDuration -= 1
            if self.remainingDuration <= 0 {
                self.timer?.invalidate()
                self.timer = nil
                self.isTimerRunning = false
            }
        }
    }

    func restartTimer() {
        startTimer(duration: selectedDuration)
    }

    func cancelTimer() {
        timer?.invalidate()
        timer = nil
        progress = 0
        remainingDuration = 0
        isTimerRunning = false
        isPaused = false
    }
}

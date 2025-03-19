package com.kontinua.readersandroidjetpack.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    var isTimerRunning by mutableStateOf(false)
    var timerDuration by mutableLongStateOf(15 * 60 * 1000L) // Default: 15 minutes
        private set // Make the setter private, but keep the property mutable.

    var timeLeftMillis by mutableLongStateOf(timerDuration)
    private var timerJob: Job? = null

    fun startTimer(initialTimeMillis: Long = timerDuration) {
        timerJob?.cancel() // Cancel any existing timer
        timeLeftMillis = initialTimeMillis
        isTimerRunning = true

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (timeLeftMillis > 0) {
                delay(100) // Check every 100 milliseconds
                timeLeftMillis -= 100
            }
            isTimerRunning = false // Timer finished
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        isTimerRunning = false
    }

    fun resumeTimer() {
        startTimer(timeLeftMillis)
    }

    fun resetTimer() {
        timerJob?.cancel()
        timeLeftMillis = timerDuration // Directly modify timerDuration
        isTimerRunning = false
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timeLeftMillis = 0
        isTimerRunning = false
    }
    fun setDurationAndReset(durationMillis: Long) {
        timerDuration = durationMillis // Use the private setter
        resetTimer() // Then, call reset
    }
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel() // Important: Cancel the job when the ViewModel is cleared
    }
}
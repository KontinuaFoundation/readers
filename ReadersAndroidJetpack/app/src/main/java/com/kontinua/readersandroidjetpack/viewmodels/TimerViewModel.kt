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
    var timerDuration by mutableLongStateOf(15 * 6 * 1000L)
        private set

    var timeLeftMillis by mutableLongStateOf(0)
        private set

    var isTimerFinished by mutableStateOf(false)
        private set

    private var timerJob: Job? = null

    fun startTimer(initialTimeMillis: Long = timerDuration) {
        timerJob?.cancel()
        isTimerFinished = false
        timeLeftMillis = initialTimeMillis
        isTimerRunning = true

        if (initialTimeMillis <= 0) {
            isTimerRunning = false
            timeLeftMillis = 0
            return
        }

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (timeLeftMillis > 0 && isTimerRunning) {
                delay(100)
                if (isTimerRunning) {
                    timeLeftMillis = (timeLeftMillis - 100).coerceAtLeast(0L)
                }
            }
            if (timeLeftMillis <= 0) {
                isTimerRunning = false
                isTimerFinished = true
                timeLeftMillis = 0
            }
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
        timeLeftMillis = 0
        isTimerRunning = false
        isTimerFinished = false
    }

    fun setDurationAndReset(durationMillis: Long) {
        timerDuration = durationMillis
        resetTimer()
        startTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
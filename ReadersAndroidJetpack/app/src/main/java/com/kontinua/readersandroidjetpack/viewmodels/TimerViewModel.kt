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
    var timerDuration by mutableLongStateOf(15 * 60 * 1000L)
        private set

    var timeLeftMillis by mutableLongStateOf(0)
        private set

    private var timerJob: Job? = null

    fun startTimer(initialTimeMillis: Long = timerDuration) {
        timerJob?.cancel()
        timeLeftMillis = initialTimeMillis
        isTimerRunning = true

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (timeLeftMillis > 0) {
                delay(100)
                timeLeftMillis -= 100
            }
            isTimerRunning = false
            timeLeftMillis = 0
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
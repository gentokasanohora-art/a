package com.emeraldrng.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emeraldrng.rng.EmeraldRNG
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RngViewModel : ViewModel() {

    private val _results = MutableStateFlow<List<EmeraldRNG.Spread>>(emptyList())
    val results: StateFlow<List<EmeraldRNG.Spread>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    data class TimerState(
        val targetFrame: Long = 0L,
        val remainingFrames: Long = 0L,
        val formattedTime: String = "00:00.00",
        val isRunning: Boolean = false,
        val progress: Float = 0f
    )
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState
    private var timerJob: Job? = null

    fun search(
        nature: Int? = null,
        minIVs: IntArray? = null,
        onlyShiny: Boolean = false,
        tid: Int = 0,
        sid: Int = 0,
        method: String = "Method1",
        maxResults: Int = 30
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _results.value = EmeraldRNG.search(
                nature = nature, minIVs = minIVs, onlyShiny = onlyShiny,
                tid = tid, sid = sid, method = method, maxResults = maxResults
            )
            _isLoading.value = false
        }
    }

    fun setTargetFrame(target: Long) {
        stopTimer()
        _timerState.value = TimerState(
            targetFrame = target,
            remainingFrames = target,
            formattedTime = formatTime(target)
        )
    }

    fun startTimer() {
        if (_timerState.value.isRunning || _timerState.value.targetFrame <= 0) return
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val target = _timerState.value.targetFrame
            val startNanos = SystemClock.elapsedRealtimeNanos()
            val fps = 59.7275
            _timerState.value = _timerState.value.copy(isRunning = true)
            while (isActive && _timerState.value.isRunning) {
                val elapsed = (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000_000.0
                val remaining = (target - (elapsed * fps).toLong()).coerceAtLeast(0L)
                val progress = if (target > 0) (1f - remaining.toFloat() / target).coerceIn(0f, 1f) else 1f
                _timerState.value = _timerState.value.copy(
                    remainingFrames = remaining,
                    formattedTime = formatTime(remaining),
                    progress = progress
                )
                if (remaining <= 0) {
                    _timerState.value = _timerState.value.copy(isRunning = false)
                    break
                }
                delay(8L)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resetTimer() {
        stopTimer()
        setTargetFrame(_timerState.value.targetFrame)
    }

    private fun formatTime(frames: Long): String {
        val sec = EmeraldRNG.framesToSeconds(frames)
        return "%02d:%05.2f".format((sec / 60).toInt(), sec % 60)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

package com.example.fp6sensors.features.vibration.presentation

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fp6sensors.features.vibration.data.recorder.SensorWavRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VibrationViewModel(
    private val recorder: SensorWavRecorder
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()
    
    private val _lastRecording = MutableStateFlow<File?>(null)
    val lastRecording = _lastRecording.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    
    private var mediaPlayer: MediaPlayer? = null

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        // Run on IO thread roughly? Recorder handles its own thread.
        // But startRecording returns File object immediately or after?
        // My implementation returns File immediately.
        val file = recorder.startRecording()
        _lastRecording.value = file
    }

    private fun stopRecording() {
        _isRecording.value = false
        recorder.stopRecording()
    }
    
    fun playRecording() {
        val file = _lastRecording.value ?: return
        if (_isPlaying.value) {
             mediaPlayer?.stop()
             mediaPlayer?.release()
             mediaPlayer = null
             _isPlaying.value = false
             return
        }
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { 
                    _isPlaying.value = false
                    it.release()
                    mediaPlayer = null
                }
            }
            _isPlaying.value = true
        } catch (e: Exception) {
            e.printStackTrace()
             _isPlaying.value = false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        recorder.stopRecording()
        mediaPlayer?.release()
    }
}

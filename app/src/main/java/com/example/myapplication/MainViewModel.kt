package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CsvLogger
import com.example.myapplication.data.LocationData
import com.example.myapplication.data.LocationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    private val locationManager: LocationManager,
    private val csvLogger: CsvLogger
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationData?>(null)
    val locationState: StateFlow<LocationData?> = _locationState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _recordedFile = MutableStateFlow<File?>(null)
    val recordedFile: StateFlow<File?> = _recordedFile.asStateFlow()

    private var clockJob: Job? = null
    private var locationJob: Job? = null
    private var csvJob: Job? = null

    init {
        startClock()
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            while (true) {
                _currentTime.value = sdf.format(Date())
                delay(50)
            }
        }
    }

    fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            locationManager.getLocationUpdates().collect { location ->
                _locationState.value = location
            }
        }
    }

    fun startRecording(fileName: String) {
        _isRecording.value = true
        val csvName = fileName.replace(".mp4", ".csv")
        csvLogger.startLogging(csvName)
        
        csvJob?.cancel()
        csvJob = viewModelScope.launch {
            locationState.collect { location ->
                if (_isRecording.value && location != null) {
                    csvLogger.logLocation(location)
                }
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        csvJob?.cancel()
        csvLogger.stopLogging()
    }

    fun setRecordedFile(file: File) {
        _recordedFile.value = file
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        locationJob?.cancel()
        csvJob?.cancel()
    }
}

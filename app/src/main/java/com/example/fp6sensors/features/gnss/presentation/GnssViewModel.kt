package com.example.fp6sensors.features.gnss.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fp6sensors.features.gnss.domain.model.LocationData
import com.example.fp6sensors.features.gnss.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GnssViewModel(
    private val repository: LocationRepository
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationData?>(null)
    val locationState: StateFlow<LocationData?> = _locationState.asStateFlow()



    fun startLocationUpdates() {
        viewModelScope.launch {
            repository.getSensorData().collect { data ->
                _locationState.value = data
            }
        }
    }
}

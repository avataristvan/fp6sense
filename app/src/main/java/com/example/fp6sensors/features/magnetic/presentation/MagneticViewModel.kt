package com.example.fp6sensors.features.magnetic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fp6sensors.features.magnetic.domain.model.MagneticData
import com.example.fp6sensors.features.magnetic.domain.repository.MagneticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MagneticViewModel(
    private val repository: MagneticRepository
) : ViewModel() {

    private val _magneticState = MutableStateFlow(MagneticData(0f, 0f, 0f, 0f))
    val magneticState: StateFlow<MagneticData> = _magneticState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMagneticData().collect {
                _magneticState.value = it
            }
        }
    }
}

package com.example.fp6sensors.features.environment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fp6sensors.features.environment.domain.model.EnvironmentData
import com.example.fp6sensors.features.environment.domain.repository.EnvironmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EnvironmentViewModel(
    private val repository: EnvironmentRepository
) : ViewModel() {

    private val _environmentState = MutableStateFlow(EnvironmentData())
    val environmentState: StateFlow<EnvironmentData> = _environmentState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getEnvironmentData().collect {
                _environmentState.value = it
            }
        }
    }
}

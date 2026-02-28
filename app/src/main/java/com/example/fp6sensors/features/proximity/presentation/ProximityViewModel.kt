package com.example.fp6sensors.features.proximity.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fp6sensors.features.proximity.domain.model.ProximityData
import com.example.fp6sensors.features.proximity.domain.repository.ProximityRepository
import com.example.fp6sensors.features.environment.domain.model.EnvironmentData
import com.example.fp6sensors.features.environment.domain.repository.EnvironmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProximityUiState(
    val distance: Float = 0f,
    val maxRange: Float = 5f,
    val lightLevel: Float = 0f,
    val referenceLux: Float? = null
) {
    val proximityIntensity: Float
        get() {
            // Only show if proximity sensor is triggered (NEAR) and we have a reference
            val isNear = distance < maxRange
            val ref = referenceLux
            if (!isNear || ref == null) return 0f

            // Logic:
            // At referenceLux (start of trigger), intensity is 0.0 ("alpha 0").
            // At 0 Lux, intensity is 1.0 ("fully red").
            // As light output goes DOWN from reference -> intensity goes UP.
            
            if (ref <= 0.1f) return 1f // If it was already pitch black, stay full red? Or maybe handled by formula.

            // Formula: (reference - current) / reference
            // Ex: Ref=100. Curr=100 -> 0/100 = 0.
            // Ex: Ref=100. Curr=50 -> 50/100 = 0.5.
            // Ex: Ref=100. Curr=0 -> 100/100 = 1.0.
            return ((ref - lightLevel) / ref).coerceIn(0f, 1f)
        }
}

class ProximityViewModel(
    private val repository: ProximityRepository,
    private val environmentRepository: EnvironmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProximityUiState())
    val uiState: StateFlow<ProximityUiState> = _uiState.asStateFlow()
    
    // To latch the lux value when proximity triggers
    private var latchedLux: Float? = null
    private var wasNear: Boolean = false

    init {
        viewModelScope.launch {
            val proximityFlow = if (repository.isSensorAvailable()) {
                repository.getSensorData()
            } else {
                MutableStateFlow(ProximityData(5f, 5f))
            }

            // Environment repo returns EnvironmentData (pressure, light)
            val lightFlow = environmentRepository.getEnvironmentData()

            combine(proximityFlow, lightFlow) { prox: ProximityData, env: EnvironmentData ->
                val currentLux = env.illuminanceLux ?: 0f
                val isNear = prox.distance < prox.maxRange
                
                // Edge detect: Far -> Near
                if (isNear && !wasNear) {
                    latchedLux = currentLux
                } else if (!isNear) {
                    latchedLux = null
                }
                wasNear = isNear

                ProximityUiState(
                    distance = prox.distance,
                    maxRange = prox.maxRange,
                    lightLevel = currentLux,
                    referenceLux = latchedLux
                )
            }.collect {
                _uiState.value = it
            }
        }
    }
}

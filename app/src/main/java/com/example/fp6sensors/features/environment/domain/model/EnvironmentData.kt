package com.example.fp6sensors.features.environment.domain.model

import com.example.fp6sensors.core.domain.model.SensorData

data class EnvironmentData(
    val pressureHpa: Float? = null,
    val illuminanceLux: Float? = null
) : SensorData

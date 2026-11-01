package app.fp6sense.features.environment.domain.model

import app.fp6sense.core.domain.model.SensorData

data class EnvironmentData(
    val pressureHpa: Float? = null,
    val illuminanceLux: Float? = null
) : SensorData

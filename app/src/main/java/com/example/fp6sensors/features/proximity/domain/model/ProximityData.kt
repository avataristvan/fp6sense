package com.example.fp6sensors.features.proximity.domain.model

import com.example.fp6sensors.core.domain.model.SensorData

data class ProximityData(
    val distance: Float, // cm
    val maxRange: Float // cm
) : SensorData {
    val isNear: Boolean
        get() = distance < maxRange
    
    // Normalized 0.0 (Far) to 1.0 (Touching/Very Close)
    // Some sensors only report 0 or 5.
    val proximityIntensity: Float
        get() {
            if (maxRange <= 0) return 0f
            return (1.0f - (distance / maxRange)).coerceIn(0f, 1f)
        }
}

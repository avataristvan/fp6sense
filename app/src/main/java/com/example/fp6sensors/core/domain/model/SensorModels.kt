package com.example.fp6sensors.core.domain.model

/**
 * Marker interface for all sensor data
 */
interface SensorData

enum class SensorAccuracy {
    HIGHEST, MEDIUM, LOW, UNRELIABLE
}

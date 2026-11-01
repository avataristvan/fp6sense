package app.fp6sense.core.domain.model

/**
 * Marker interface for all sensor data
 */
interface SensorData

enum class SensorAccuracy {
    HIGHEST, MEDIUM, LOW, UNRELIABLE
}

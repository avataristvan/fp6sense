package app.fp6sense.features.vital.domain.model

import app.fp6sense.core.domain.model.SensorData

data class VitalData(
    val heartSignal: Float = 0f,    // Filtered Accel (High Pass)
    val breathSignal: Float = 0f,   // Filtered Accel/Gyro (Low Pass)
    val bpm: Int = 0,               // Heart Rate
    val rpm: Int = 0,               // Respiration Rate
    val confidence: Float = 0f,     // Signal quality (0.0 - 1.0)
    val timestamp: Long = System.currentTimeMillis()
) : SensorData

package app.fp6sense.features.motion.domain.model

import app.fp6sense.core.domain.model.SensorData

data class MotionData(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    // Backwards compatibility aliases (mapped to Accel)
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) : SensorData

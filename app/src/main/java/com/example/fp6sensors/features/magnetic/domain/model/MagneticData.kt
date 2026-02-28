package com.example.fp6sensors.features.magnetic.domain.model

import com.example.fp6sensors.core.domain.model.SensorData

data class MagneticData(
    val x: Float,
    val y: Float,
    val z: Float,
    val totalStrength: Float, // Tesla or MicroTesla
    val azimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val rotationMatrix: FloatArray = FloatArray(9)
) : SensorData

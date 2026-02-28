package com.example.fp6sensors.core.domain.repository

import com.example.fp6sensors.core.domain.model.SensorData
import kotlinx.coroutines.flow.Flow

interface SensorRepository<T : SensorData> {
    fun getSensorData(): Flow<T>
    fun isSensorAvailable(): Boolean
}

package app.fp6sense.core.domain.repository

import app.fp6sense.core.domain.model.SensorData
import kotlinx.coroutines.flow.Flow

interface SensorRepository<T : SensorData> {
    fun getSensorData(): Flow<T>
    fun isSensorAvailable(): Boolean
}

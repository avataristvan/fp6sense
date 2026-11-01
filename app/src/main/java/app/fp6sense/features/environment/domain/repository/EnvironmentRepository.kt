package app.fp6sense.features.environment.domain.repository

import app.fp6sense.core.domain.repository.SensorRepository
import app.fp6sense.features.environment.domain.model.EnvironmentData
import kotlinx.coroutines.flow.Flow

interface EnvironmentRepository {
    fun getEnvironmentData(): Flow<EnvironmentData>
}

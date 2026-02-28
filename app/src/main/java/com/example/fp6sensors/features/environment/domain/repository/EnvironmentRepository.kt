package com.example.fp6sensors.features.environment.domain.repository

import com.example.fp6sensors.core.domain.repository.SensorRepository
import com.example.fp6sensors.features.environment.domain.model.EnvironmentData
import kotlinx.coroutines.flow.Flow

interface EnvironmentRepository {
    fun getEnvironmentData(): Flow<EnvironmentData>
}

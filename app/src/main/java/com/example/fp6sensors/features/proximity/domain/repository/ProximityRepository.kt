package com.example.fp6sensors.features.proximity.domain.repository

import com.example.fp6sensors.core.domain.repository.SensorRepository
import com.example.fp6sensors.features.proximity.domain.model.ProximityData
import kotlinx.coroutines.flow.Flow

interface ProximityRepository : SensorRepository<ProximityData> {
    // getSensorData returns Flow<ProximityData>
}

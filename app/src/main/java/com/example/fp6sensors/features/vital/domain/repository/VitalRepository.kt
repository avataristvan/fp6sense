package com.example.fp6sensors.features.vital.domain.repository

import com.example.fp6sensors.core.domain.repository.SensorRepository
import com.example.fp6sensors.features.vital.domain.model.VitalData

interface VitalRepository : SensorRepository<VitalData> {
    // Inherits getSensorData(): Flow<VitalData>
}

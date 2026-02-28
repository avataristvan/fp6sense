package com.example.fp6sensors.features.gnss.domain.repository

import com.example.fp6sensors.core.domain.repository.SensorRepository
import com.example.fp6sensors.features.gnss.domain.model.LocationData
import kotlinx.coroutines.flow.Flow

interface LocationRepository : SensorRepository<LocationData> {
    // getSensorData() is inherited
}

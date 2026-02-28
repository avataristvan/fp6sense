package com.example.fp6sensors.features.proximity.data.repository

import android.content.Context
import android.hardware.Sensor
import com.example.fp6sensors.core.data.AndroidSensorRepository
import com.example.fp6sensors.features.proximity.domain.model.ProximityData
import com.example.fp6sensors.features.proximity.domain.repository.ProximityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProximityRepositoryImpl(
    context: Context
) : AndroidSensorRepository(context, Sensor.TYPE_PROXIMITY), ProximityRepository {

    override fun getSensorData(): Flow<ProximityData> {
        val max = sensor?.maximumRange ?: 5f
        return getSensorValues().map { values ->
            ProximityData(
                distance = values[0],
                maxRange = max
            )
        }
    }
}

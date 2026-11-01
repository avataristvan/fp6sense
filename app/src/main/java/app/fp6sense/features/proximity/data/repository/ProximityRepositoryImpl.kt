package app.fp6sense.features.proximity.data.repository

import android.content.Context
import android.hardware.Sensor
import app.fp6sense.core.data.AndroidSensorRepository
import app.fp6sense.features.proximity.domain.model.ProximityData
import app.fp6sense.features.proximity.domain.repository.ProximityRepository
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

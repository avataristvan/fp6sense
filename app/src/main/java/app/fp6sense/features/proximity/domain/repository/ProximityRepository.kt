package app.fp6sense.features.proximity.domain.repository

import app.fp6sense.core.domain.repository.SensorRepository
import app.fp6sense.features.proximity.domain.model.ProximityData
import kotlinx.coroutines.flow.Flow

interface ProximityRepository : SensorRepository<ProximityData> {
    // getSensorData returns Flow<ProximityData>
}

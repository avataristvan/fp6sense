package app.fp6sense.features.gnss.domain.repository

import app.fp6sense.core.domain.repository.SensorRepository
import app.fp6sense.features.gnss.domain.model.LocationData
import kotlinx.coroutines.flow.Flow

interface LocationRepository : SensorRepository<LocationData> {
    // getSensorData() is inherited
}

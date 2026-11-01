package app.fp6sense.features.vital.domain.repository

import app.fp6sense.core.domain.repository.SensorRepository
import app.fp6sense.features.vital.domain.model.VitalData

interface VitalRepository : SensorRepository<VitalData> {
    // Inherits getSensorData(): Flow<VitalData>
}

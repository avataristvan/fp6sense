package app.fp6sense.features.motion.domain.repository

import app.fp6sense.core.domain.repository.SensorRepository
import app.fp6sense.features.motion.domain.model.MotionData

interface MotionRepository : SensorRepository<MotionData> {
    // getSensorData returns Flow<MotionData>
}

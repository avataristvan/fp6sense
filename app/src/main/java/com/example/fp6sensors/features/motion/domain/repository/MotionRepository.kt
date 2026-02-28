package com.example.fp6sensors.features.motion.domain.repository

import com.example.fp6sensors.core.domain.repository.SensorRepository
import com.example.fp6sensors.features.motion.domain.model.MotionData

interface MotionRepository : SensorRepository<MotionData> {
    // getSensorData returns Flow<MotionData>
}

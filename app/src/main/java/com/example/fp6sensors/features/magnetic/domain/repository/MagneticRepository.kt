package com.example.fp6sensors.features.magnetic.domain.repository

import com.example.fp6sensors.features.magnetic.domain.model.MagneticData
import kotlinx.coroutines.flow.Flow

interface MagneticRepository {
    fun getMagneticData(): Flow<MagneticData>
}

package app.fp6sense.features.magnetic.domain.repository

import app.fp6sense.features.magnetic.domain.model.MagneticData
import kotlinx.coroutines.flow.Flow

interface MagneticRepository {
    fun getMagneticData(): Flow<MagneticData>
}

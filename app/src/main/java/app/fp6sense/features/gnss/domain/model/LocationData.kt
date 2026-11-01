package app.fp6sense.features.gnss.domain.model

import app.fp6sense.core.domain.model.SensorData

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val satelliteCount: Int,
    val constellationTypes: List<String>, // e.g., "GPS", "Galileo"
    val satellites: List<Satellite> = emptyList()
) : SensorData

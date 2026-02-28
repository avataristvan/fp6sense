package com.example.fp6sensors.features.gnss.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.fp6sensors.features.gnss.domain.model.LocationData
import com.example.fp6sensors.features.gnss.domain.model.Satellite
import com.example.fp6sensors.features.gnss.domain.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors

class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun isSensorAvailable(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("MissingPermission") // Check permissions in UI
    override fun getSensorData(): Flow<LocationData> = callbackFlow {
        if (!isSensorAvailable()) {
            close()
            return@callbackFlow
        }

        var currentSatellites = 0
        var currentConstellations = emptyList<String>()
        var currentSatelliteList = emptyList<Satellite>()

        val gnssCallback = @RequiresApi(Build.VERSION_CODES.N)
        object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                currentSatellites = status.satelliteCount
                val types = mutableSetOf<String>()
                val satellites = mutableListOf<Satellite>()
                
                for (i in 0 until status.satelliteCount) {
                    val constellationName = getConstellationName(status.getConstellationType(i))
                    types.add(constellationName)
                    
                    satellites.add(
                        Satellite(
                            svid = status.getSvid(i),
                            constellation = constellationName,
                            cn0DbHz = status.getCn0DbHz(i),
                            elevationDegrees = status.getElevationDegrees(i),
                            azimuthDegrees = status.getAzimuthDegrees(i),
                            usedInFix = status.usedInFix(i)
                        )
                    )
                }
                currentConstellations = types.toList()
                currentSatelliteList = satellites.sortedByDescending { it.usedInFix }
            }
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(
                    LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        accuracy = location.accuracy,
                        speed = location.speed,
                        bearing = location.bearing,
                        satelliteCount = currentSatellites,
                        constellationTypes = currentConstellations,
                        satellites = currentSatelliteList
                    )
                )
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.registerGnssStatusCallback(Executors.newSingleThreadExecutor(), gnssCallback)
            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f,
                locationListener
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            locationManager.removeUpdates(locationListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.unregisterGnssStatusCallback(gnssCallback)
            }
        }
    }

    private fun getConstellationName(id: Int): String {
        return when (id) {
            GnssStatus.CONSTELLATION_GPS -> "GPS"
            GnssStatus.CONSTELLATION_SBAS -> "SBAS"
            GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
            GnssStatus.CONSTELLATION_QZSS -> "QZSS"
            GnssStatus.CONSTELLATION_BEIDOU -> "BEIDOU"
            GnssStatus.CONSTELLATION_GALILEO -> "GALILEO"
            GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
            else -> "UNKNOWN"
        }
    }
}

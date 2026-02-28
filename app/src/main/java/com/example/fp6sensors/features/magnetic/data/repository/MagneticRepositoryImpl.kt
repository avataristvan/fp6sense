package com.example.fp6sensors.features.magnetic.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.fp6sensors.features.magnetic.domain.model.MagneticData
import com.example.fp6sensors.features.magnetic.domain.repository.MagneticRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class MagneticRepositoryImpl(
    private val context: Context
) : MagneticRepository {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun getMagneticData(): Flow<MagneticData> = callbackFlow {
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        gravity = it.values.clone()
                    } else if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        geomagnetic = it.values.clone()
                    }

                    if (gravity != null && geomagnetic != null) {
                        val R = FloatArray(9)
                        val I = FloatArray(9)
                        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(R, orientation)
                            
                            val x = geomagnetic!![0]
                            val y = geomagnetic!![1]
                            val z = geomagnetic!![2]
                            val strength = sqrt(x * x + y * y + z * z)

                            trySend(
                                MagneticData(
                                    x = x,
                                    y = y,
                                    z = z,
                                    totalStrength = strength,
                                    azimuth = orientation[0], // Radians
                                    pitch = orientation[1],
                                    roll = orientation[2],
                                    rotationMatrix = R
                                )
                            )
                        }
                    } else if (geomagnetic != null) {
                         // Fallback just mag data if no accel yet
                         val x = geomagnetic!![0]
                         val y = geomagnetic!![1]
                         val z = geomagnetic!![2]
                         val strength = sqrt(x * x + y * y + z * z)
                         trySend(MagneticData(x,y,z, strength))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        if (magSensor != null) {
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (accelSensor != null) {
             sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}

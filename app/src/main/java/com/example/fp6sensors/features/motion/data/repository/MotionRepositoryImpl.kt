package com.example.fp6sensors.features.motion.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.fp6sensors.features.motion.domain.model.MotionData
import com.example.fp6sensors.features.motion.domain.repository.MotionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MotionRepositoryImpl(
    private val context: Context
) : MotionRepository {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun getSensorData(): Flow<MotionData> = callbackFlow {
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        // State to hold latest values
        var lastAccel = FloatArray(3)
        var lastGyro = FloatArray(3)
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        lastAccel = it.values.clone()
                    } else if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                        lastGyro = it.values.clone()
                    }
                    
                    // Emit combined data on every update (could throttle if needed)
                    trySend(
                        MotionData(
                            accelX = lastAccel[0],
                            accelY = lastAccel[1],
                            accelZ = lastAccel[2],
                            gyroX = lastGyro[0],
                            gyroY = lastGyro[1],
                            gyroZ = lastGyro[2],
                            // Legacy mapping to Accel
                            x = lastAccel[0],
                            y = lastAccel[1],
                            z = lastAccel[2]
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        gyroSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    override fun isSensorAvailable(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ||
               sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }
}

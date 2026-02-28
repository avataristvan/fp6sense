package com.example.fp6sensors.features.environment.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.fp6sensors.features.environment.domain.model.EnvironmentData
import com.example.fp6sensors.features.environment.domain.repository.EnvironmentRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class EnvironmentRepositoryImpl(
    private val context: Context
) : EnvironmentRepository {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun getEnvironmentData(): Flow<EnvironmentData> = callbackFlow {
        var currentPressure: Float? = null
        var currentLight: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    var changed = false
                    if (it.sensor.type == Sensor.TYPE_PRESSURE) {
                        currentPressure = it.values[0]
                        changed = true
                    } else if (it.sensor.type == Sensor.TYPE_LIGHT) {
                        currentLight = it.values[0]
                        changed = true
                    }
                    
                    if (changed) {
                        trySend(EnvironmentData(currentPressure, currentLight))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (pressureSensor != null) {
            sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}

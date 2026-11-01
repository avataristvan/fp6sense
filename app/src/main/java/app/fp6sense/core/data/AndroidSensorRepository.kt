package app.fp6sense.core.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

abstract class AndroidSensorRepository(
    private val context: Context,
    private val sensorType: Int
) {
    protected val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    protected val sensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(sensorType)
    }

    fun isSensorAvailable(): Boolean = sensor != null

    protected fun getSensorValues(): Flow<FloatArray> = callbackFlow {
        if (!isSensorAvailable()) {
             close()
             return@callbackFlow
        }
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(it.values.clone()) // Clone is important
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy if needed
            }
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}

package com.example.fp6sensors.features.vital.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.fp6sensors.features.vital.domain.model.VitalData
import com.example.fp6sensors.features.vital.domain.repository.VitalRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.sqrt

class VitalRepositoryImpl(
    private val context: Context
) : VitalRepository {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun getSensorData(): Flow<VitalData> = callbackFlow {
        // Linear Acceleration (Gravity excluded) is best for Seismocardiography (SCG)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        // Signal Buffers
        // (Cleaned up unused variables)
        
        // Simple IIR Filter State
        // Breath: Low Pass (~0.5Hz)
        var breathLowPass = 0f
        
        // Heart: Band Pass (0.8Hz - 10Hz)
        var heartBandPass = 0f
        
        
        // Peak Detection State
        var lastHeartPeakTime = 0L
        val heartRateBuffer = ArrayDeque<Int>() // Store last 8 bpm values
        
        var lastBreathPeakTime = 0L
        val breathRateBuffer = ArrayDeque<Int>()
        

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                        // SCG is mostly visible in Z axis (dorso-ventral) when lying down
                        val z = it.values[2] 
                        
                        // --- Signal Processing ---
                        
                        // 1. Breath Signal (Low Pass Filter, alpha ~ 0.05)
                        breathLowPass = 0.95f * breathLowPass + 0.05f * z
                        
                        // 2. Heart Signal (High Pass -> Band Pass)
                        // Remove DC/Breath
                        val dc = breathLowPass
                        val ac = z - dc
                        
                        // Smoothing (Low Pass on AC) to remove noise > 20Hz
                        heartBandPass = 0.8f * heartBandPass + 0.2f * ac
                        
                        
                        // --- Peak Detection (Simple Zero Crossing / Threshold) ---
                        val time = System.currentTimeMillis()
                        
                        // Heart Peak
                        if (heartBandPass > 0.4f && (time - lastHeartPeakTime) > 350) { // Limit to ~170 BPM max
                            if (lastHeartPeakTime != 0L) {
                                val interval = time - lastHeartPeakTime
                                val instantBpm = (60000 / interval).toInt()
                                if (instantBpm in 40..180) {
                                    heartRateBuffer.addLast(instantBpm)
                                    if (heartRateBuffer.size > 8) heartRateBuffer.removeFirst()
                                }
                            }
                            lastHeartPeakTime = time
                        }
                        
                        // Breath Peak
                         if (breathLowPass > 0.6f && (time - lastBreathPeakTime) > 2500) { // Limit to ~24 RPM max
                            if (lastBreathPeakTime != 0L) {
                                val interval = time - lastBreathPeakTime
                                val instantRpm = (60000 / interval).toInt()
                                if (instantRpm in 6..40) {
                                    breathRateBuffer.addLast(instantRpm)
                                    if (breathRateBuffer.size > 4) breathRateBuffer.removeFirst()
                                }
                            }
                            lastBreathPeakTime = time
                         }
                         
                         // Calculate stable Averages
                         val avgBpm = if (heartRateBuffer.isNotEmpty()) heartRateBuffer.average().toInt() else 0
                         val avgRpm = if (breathRateBuffer.isNotEmpty()) breathRateBuffer.average().toInt() else 0
                         
                         // Confidence based on buffer fullness
                         val conf = (heartRateBuffer.size / 8f) * 0.7f + (breathRateBuffer.size / 4f) * 0.3f
                         
                         trySend(
                            VitalData(
                                heartSignal = heartBandPass * 5f,
                                breathSignal = breathLowPass * 2f,
                                bpm = avgBpm,
                                rpm = avgRpm,
                                confidence = conf,
                                timestamp = time
                            )
                        )
                    } 
                    
                    if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        // Use Magnetic changes to validate or filter
                        // For now, could modulate confidence
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) } // fast for heart
        magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    override fun isSensorAvailable(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
    }
}

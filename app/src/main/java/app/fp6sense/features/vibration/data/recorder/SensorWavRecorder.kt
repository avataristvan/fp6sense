package app.fp6sense.features.vibration.data.recorder

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sign

class SensorWavRecorder(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var isRecording = false
    private var outputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    
    // Audio configuration
    private val sampleRate = 44100
    private val bitsPerSample: Short = 16
    private val channels: Short = 2 // Stereo: Left=Accel, Right=Gyro

    // Sensor State
    private var lastAccelVal = 0f
    private var lastGyroVal = 0f
    
    // Threading
    private var recordingThread: Thread? = null

    fun startRecording(): File {
        val fileName = "vibration_${System.currentTimeMillis()}.wav"
        outputFile = File(context.cacheDir, fileName)
        fileOutputStream = FileOutputStream(outputFile)
        
        // Write placeholder header
        writeWavHeader(fileOutputStream!!, 0)
        
        isRecording = true
        
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        // Use a dedicated thread for "Audio Generation" loop
        recordingThread = Thread {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        if (it.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                            // Combine axes for magnitude, or take Z axis?
                            // Let's take Z (most sensitive to taps on table)
                            lastAccelVal = it.values[2]
                        }
                        if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                             // Combine axes or simple X?
                             // Let's use X axis rotation
                             lastGyroVal = it.values[0]
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
            
            try {
                val bufferSize = 1024
                val byteBuffer = ByteArray(bufferSize * 2 * 2) // samples * channels * bytes
                
                // Sample generation loop
                // We generate 44100 samples per second.
                // Loop sleep time?
                val nsPerSample = 1_000_000_000.0 / sampleRate
                var nextSampleTime = System.nanoTime()
                
                while (isRecording) {
                    val now = System.nanoTime()
                    if (now >= nextSampleTime) {
                        // Generate ONE sample (Stereo)
                        // In a real optimized app, we would batch this.
                        // For PoC, we loop tight or batch slightly.
                        
                        // Current Sensor Values (Holding last value is basically "Sample and Hold" without interpolation)
                        // True interpolation requires timestamps, but simple Sample-Hold at 44k from 200Hz input
                        // creates "steps". Ideally we smooth it.
                        // Let's use Linear Smoothing/Slew Limiting to simulate interpolation casually.
                        // actually, let's just write the current value. The steps create high-freq aliases which might sound interesting/robotic.
                        
                        // Process Left Channel (Accel)
                        val audioLeft = processSignal(lastAccelVal, 5.0f) // Range +/- 5 m/s^2
                        
                        // Process Right Channel (Gyro)
                        val audioRight = processSignal(lastGyroVal, 5.0f) // Range +/- 5 rad/s
                        
                        // Write to stream
                        // Little Endian 16-bit
                        // Left
                        val leftShort = (audioLeft * 32767).toInt().coerceIn(-32768, 32767).toShort()
                         fileOutputStream?.write(shortToBytes(leftShort))
                         
                         // Right
                        val rightShort = (audioRight * 32767).toInt().coerceIn(-32768, 32767).toShort()
                         fileOutputStream?.write(shortToBytes(rightShort))
                        
                        nextSampleTime += nsPerSample.toLong()
                    } else {
                         // Yield slightly if ahead (busy wait is bad for battery but good for timing)
                         // 44kHz = 22us per sample. Thread.sleep is 1ms minimum.
                         // Must busy wait or batch.
                         // Let's batch simple write: generate 100 samples at once based on current state.
                         // 100 samples = ~2.2ms.
                         for (i in 0 until 100) {
                              val audioLeft = processSignal(lastAccelVal, 5.0f)
                              val audioRight = processSignal(lastGyroVal, 5.0f)
                              
                              val leftShort = (audioLeft * 32767).toInt().coerceIn(-32768, 32767).toShort()
                              fileOutputStream?.write(shortToBytes(leftShort))
                              
                              val rightShort = (audioRight * 32767).toInt().coerceIn(-32768, 32767).toShort()
                              fileOutputStream?.write(shortToBytes(rightShort))
                         }
                         nextSampleTime += (nsPerSample * 100).toLong()
                         Thread.sleep(1) // Sleep 1ms
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                sensorManager.unregisterListener(listener)
                fileOutputStream?.close()
                updateWavHeader(outputFile!!)
            }
        }
        recordingThread?.start()
        
        return outputFile!!
    }

    fun stopRecording() {
        isRecording = false
        try {
            recordingThread?.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    
    private fun processSignal(raw: Float, range: Float): Float {
        // Logarithmic Companding
        // y = sign(x) * ln(1 + |x| * k) / ln(1 + range * k)
        val k = 100f // High gain
        val numerator = ln(1f + abs(raw) * k)
        val denominator = ln(1f + range * k)
        return (numerator / denominator) * raw.sign
    }

    private fun shortToBytes(s: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(s).array()
    }
    
    private fun writeWavHeader(out: FileOutputStream, totalAudioLen: Long) {
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = sampleRate.toLong()
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()
        
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte() // data
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        
        out.write(header, 0, 44)
    }

    private fun updateWavHeader(file: File) {
        val fileLength = file.length()
        val dataLength = fileLength - 44
        try {
            val randomAccessFile = RandomAccessFile(file, "rw")
            randomAccessFile.seek(4) // Write file length
            // RIFF chunk size
            val totalDataLen = (fileLength - 8).toInt()
             randomAccessFile.write(intToBytes(totalDataLen), 0, 4)
             
            randomAccessFile.seek(40) // Write data length
            randomAccessFile.write(intToBytes(dataLength.toInt()), 0, 4)
            
            randomAccessFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun intToBytes(i: Int): ByteArray {
         return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array()
    }
}

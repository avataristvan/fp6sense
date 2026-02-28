package com.example.fp6sensors.features.infrared.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

class RawCameraManager(
    private val context: Context,
    private val renderer: InfraredGpuRenderer
) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun start() {
        startBackgroundThread()
        openCamera()
    }

    fun stop() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
        stopBackgroundThread()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraRawBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: return

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startCaptureSession() {
        val device = cameraDevice ?: return
        
        // Find best RAW size
        val chars = cameraManager.getCameraCharacteristics(device.id)
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        
        // We prefer RAW_SENSOR (16-bit) or RAW10
        val rawFormat = if (map?.isOutputSupportedFor(ImageFormat.RAW_SENSOR) == true) {
            ImageFormat.RAW_SENSOR
        } else if (map?.isOutputSupportedFor(ImageFormat.RAW10) == true) {
            ImageFormat.RAW10
        } else {
            Log.e("RawCamera", "RAW not supported")
            return
        }

        val sizes = map?.getOutputSizes(rawFormat) ?: return
        val size = sizes.first() // Largest size usually
        
        renderer.width = size.width
        renderer.height = size.height
        
        imageReader = ImageReader.newInstance(size.width, size.height, rawFormat, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val rowStride = plane.rowStride
                    
                    // Pass stride and buffer
                    renderer.rowStride = rowStride
                    renderer.rawBuffer = buffer
                    
                    image.close()
                }
            }, backgroundHandler)
        }

        val surface = imageReader!!.surface
        
        device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder.addTarget(surface)
                
                // Disable auto treatments to get "realer" data
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 10000000L) // 10ms for demo
                
                session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("RawCamera", "Session configuration failed")
            }
        }, backgroundHandler)
    }
}

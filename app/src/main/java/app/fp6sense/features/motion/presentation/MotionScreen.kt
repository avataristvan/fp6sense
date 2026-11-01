package app.fp6sense.features.motion.presentation

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MotionScreen(
    viewModel: MotionViewModel
) {
    val motionData by viewModel.motionState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Motion (Accel & Gyro)",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
             Motion3DView(
                 accelX = motionData.accelX,
                 accelY = motionData.accelY,
                 accelZ = motionData.accelZ,
                 gyroX = motionData.gyroX,
                 gyroY = motionData.gyroY,
                 gyroZ = motionData.gyroZ
             )
        }
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Accelerometer (m/s²)", style = MaterialTheme.typography.labelLarge)
            Text("X: ${"%.2f".format(motionData.accelX)} Y: ${"%.2f".format(motionData.accelY)} Z: ${"%.2f".format(motionData.accelZ)}")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Gyroscope (rad/s)", style = MaterialTheme.typography.labelLarge)
            Text("X: ${"%.2f".format(motionData.gyroX)} Y: ${"%.2f".format(motionData.gyroY)} Z: ${"%.2f".format(motionData.gyroZ)}")
        }
    }
}

@Composable
fun Motion3DView(
    accelX: Float, accelY: Float, accelZ: Float,
    gyroX: Float, gyroY: Float, gyroZ: Float
) {
    val renderer = remember { MotionGpuRenderer() }
    
    LaunchedEffect(accelX, accelY, accelZ, gyroX, gyroY, gyroZ) {
        renderer.accelX = accelX
        renderer.accelY = accelY
        renderer.accelZ = accelZ
        renderer.gyroX = gyroX
        renderer.gyroY = gyroY
        renderer.gyroZ = gyroZ
    }

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

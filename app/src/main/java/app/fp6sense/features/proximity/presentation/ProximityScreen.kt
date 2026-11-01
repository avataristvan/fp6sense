package app.fp6sense.features.proximity.presentation

import android.Manifest
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProximityScreen(
    viewModel: ProximityViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showRawData by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview()
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Text("Camera permission required", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }

        // Overlay: Redness based on Light Sensor (Lux)
        // logic is now in ViewModel's ProximityUiState
        val intensity = uiState.proximityIntensity
        
        // Visuals
        // 1. Red Tint Overlay (Alpha increases)
        val redAlpha = intensity * 0.9f // Max 90% opacity red
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red.copy(alpha = redAlpha))
        )

        // 2. Narrowing Effect (Borders growing in)
        // Max border width = half of screen width/height (full cover)
        // Let's emulate a vignette or thick borders
        val borderWidth = 100.dp * intensity // Grows up to 100dp
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = borderWidth,
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        radius = 800f // Arbitrary, improves with real metrics
                    ),
                    shape = androidx.compose.ui.graphics.RectangleShape // Or CircleShape for iris effect
                )
        )
        
        // UI Controls on top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                text = "Proximity / Camera Overlay (Lux Control)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show Raw Data", color = Color.White)
                Switch(
                    checked = showRawData,
                    onCheckedChange = { showRawData = it },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            if (showRawData) {
                Text(
                    text = "Distance: ${uiState.distance} cm / Light: ${uiState.lightLevel} lux",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                preview.setSurfaceProvider(previewView.surfaceProvider)

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = Modifier.fillMaxSize()
    )
}

package app.fp6sense.features.vibration.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VibrationScreen(
    viewModel: VibrationViewModel
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lastFile by viewModel.lastRecording.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Vibration & Seismic Mic",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Records Micro-Movements (Accel/Gyro) as Audio.\nLeft Channel: Accelerometer\nRight Channel: Gyroscope",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Record Button
        Button(
            onClick = { viewModel.toggleRecording() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(120.dp).clip(CircleShape)
        ) {
            Text(if (isRecording) "STOP" else "REC")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (lastFile != null) {
            Text("Last Recording: ${(lastFile!!.length() / 1024)} KB")
            
            Button(
                onClick = { viewModel.playRecording() },
                enabled = !isRecording
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (isPlaying) "Stop Playing" else "Play Sound")
            }
        }
        
        if (isRecording) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recording... Tap or scratch the phone!", color = Color.Red)
        }
    }
}

package app.fp6sense.features.vital.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.ln
import kotlin.math.sign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.Alignment
import app.fp6sense.features.vital.presentation.components.Rotatable3DGraph

@Composable
fun VitalScreen(
    viewModel: VitalViewModel
) {
    val vitalData by viewModel.vitalState.collectAsState()
    val isMeasuring by viewModel.isMeasuring.collectAsState()
    
    // View Mode: 0 = 2D, 1 = 3D
    var selectedTabIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val tabs = listOf("2D Graphs", "3D Diagram")

    // History buffers for plotting (e.g., last 300 points)
    val heartHistory = remember { mutableStateListOf<Float>() }
    val breathHistory = remember { mutableStateListOf<Float>() }
    
    // Only update history if we have a new timestamp AND we are measuring
    // Actually, viewModel stops emitting if !isMeasuring, but check specifically for data continuity if needed.
    // Since viewModel stops updating _vitalState, vitalData won't change, so this block won't run effectively or adds dupes?
    // vitalData is StateFlow. collectAsState triggers on change. 
    // If viewModel stops updating, vitalData remains same.
    // BUT we need to ensure we don't add duplicate points if re-composed.
    // Ideally we listen to a flow of events or data points, but here we scan vitalData.
    // Let's rely on viewModel updating the state.
    
    // We need to use a side-effect to append to history to strictly follow data updates.
    // But for now, keeping existing logic structure:
    
    // State flow emissions might be "distinctUntilChanged" by default or implementation.
    // If data is same, collectAsState might not trigger?
    // Let's assume standard behavior.
    
    // We should probably rely on a LaunchedEffect collecting the flow directly for history to be robust,
    // but the existing code used compposition side-effect (which is bad practice but pre-existing).
    // I will refactor strictly if needed, but let's stick to the pattern but guard it.
    
    val currentTimestamp = vitalData.timestamp
    androidx.compose.runtime.LaunchedEffect(currentTimestamp) {
        if (currentTimestamp > 0 && isMeasuring) {
             heartHistory.add(vitalData.heartSignal)
            if (heartHistory.size > 300) heartHistory.removeAt(0)
            
            breathHistory.add(vitalData.breathSignal)
            if (breathHistory.size > 300) breathHistory.removeAt(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
            Text(
                text = "Vital Signs Monitor",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // 2D Graphs Mode
                Text(
                    text = "Instructions: Lie down. Place phone on chest (sternum). Hold breath for calibration.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Heart Graph
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Heart Signal (SCG - Logarithmic)", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                        SignalGraph(data = heartHistory, color = Color.Red, range = 5f)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Breath Graph
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                     Column(modifier = Modifier.padding(8.dp)) {
                        Text("Breath Signal (Respiration - Logarithmic)", color = Color.Cyan, style = MaterialTheme.typography.labelSmall)
                        SignalGraph(data = breathHistory, color = Color.Cyan, range = 2f)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Metrics
                Text(
                    text = "Heart Rate: ${if (vitalData.bpm > 0) "${vitalData.bpm} BPM" else "Analyzing..."}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (vitalData.bpm > 0) Color.Red else Color.Gray
                )
                Text(
                    text = "Respiration: ${if (vitalData.rpm > 0) "${vitalData.rpm} RPM" else "Analyzing..."}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (vitalData.rpm > 0) Color.Cyan else Color.Gray
                )
            } else {
                // 3D Diagram Mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Take up remaining space
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Rotatable3DGraph(
                            heartData = heartHistory,
                            breathData = breathHistory,
                            modifier = Modifier.padding(16.dp)
                        )
                        
                        Text(
                            text = "3D Phase Space (Touch to Rotate)",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                        )
                    }
                }
            }
            
            if (vitalData.confidence > 0.1f) {
                Text(
                    text = "Signal Quality: ${(vitalData.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        
        // Snapshot FAB
        FloatingActionButton(
            onClick = { viewModel.toggleMeasuring() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (isMeasuring) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
        ) {
            Icon(
                imageVector = if (isMeasuring) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isMeasuring) "Pause" else "Resume"
            )
        }
    }
}

@Composable
fun SignalGraph(data: List<Float>, color: Color, range: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (data.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val stepX = width / 300f // Show 300 points
        val midY = height / 2f
        
        val path = Path()
        
        data.forEachIndexed { index, value ->
            // Symmetric Log Transform: y' = sign(y) * ln(1 + |y| * k)
            // k defines the "zoom" on small differences. Higher k = more zoom at zero.
            val k = 100f 
            val logVal = ln(1f + value.absoluteValue * k)
            val signedLogVal = logVal * value.sign
            
            // Normalize based on log(1 + range * k)
            val maxLog = ln(1f + range * k)
            val normalizedY = midY - (signedLogVal / maxLog * (height / 2f))
            
            val x = index * stepX
            
            if (index == 0) path.moveTo(x, normalizedY)
            else path.lineTo(x, normalizedY)
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )
    }
}

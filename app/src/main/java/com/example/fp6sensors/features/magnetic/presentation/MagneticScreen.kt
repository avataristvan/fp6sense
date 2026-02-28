package com.example.fp6sensors.features.magnetic.presentation
import android.opengl.GLSurfaceView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun MagneticScreen(
    viewModel: MagneticViewModel
) {
    val data by viewModel.magneticState.collectAsState()
    var showRawData by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Magnetic & Compass",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        // Show Raw Data Toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Raw Data", modifier = Modifier.weight(1f))
            Switch(
                checked = showRawData,
                onCheckedChange = { showRawData = it }
            )
        }

        // Hall / Strength Visualization
        Card(modifier = Modifier.fillMaxWidth()) {
             Column {
                 HallSensorSection(data.totalStrength)
                 if (showRawData) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text("Raw Magnetic Field (µT):", style = MaterialTheme.typography.labelLarge)
                         Text("X: ${"%.2f".format(data.x)}")
                         Text("Y: ${"%.2f".format(data.y)}")
                         Text("Z: ${"%.2f".format(data.z)}")
                         Text("Total: ${"%.2f".format(data.totalStrength)}")
                     }
                 }
             }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Compass
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Compass", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Magnetic3DView(
                        rotationMatrix = data.rotationMatrix,
                        fieldStrength = data.totalStrength,
                        x = data.x,
                        y = data.y,
                        z = data.z
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                     text = "${Math.toDegrees(data.azimuth.toDouble()).toInt()}°",
                     style = MaterialTheme.typography.headlineSmall
                )
                
                if (showRawData) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                        Text("Raw Orientation (Deg):", style = MaterialTheme.typography.labelLarge)
                        Text("Azimuth: ${"%.1f".format(Math.toDegrees(data.azimuth.toDouble()))}°")
                        Text("Pitch: ${"%.1f".format(Math.toDegrees(data.pitch.toDouble()))}°")
                        Text("Roll: ${"%.1f".format(Math.toDegrees(data.roll.toDouble()))}°")
                    }
                }
            }
        }
    }
}

@Composable
fun Magnetic3DView(rotationMatrix: FloatArray, fieldStrength: Float, x: Float, y: Float, z: Float) {
    val renderer = remember { MagneticGpuRenderer() }
    
    // Update renderer properties when data changes
    LaunchedEffect(rotationMatrix, fieldStrength, x, y, z) {
        if (rotationMatrix.isNotEmpty()) {
             System.arraycopy(rotationMatrix, 0, renderer.rotationMatrix, 0, rotationMatrix.size)
        }
        renderer.fieldStrength = fieldStrength
        renderer.magX = x
        renderer.magY = y
        renderer.magZ = z
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

@Composable
fun HallSensorSection(strength: Float) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Field Strength (Logarithmic)")
        val safeStrength = if (strength < 1f) 1f else strength
        val logVal = log10(safeStrength)
        val maxLog = 4f // 10000 uT
        val progress = (logVal / maxLog).coerceIn(0f, 1f)

        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
            )
            Text(
                text = "${strength.toInt()} µT",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun AnalogCompass(azimuth: Float, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val azimuthDeg = Math.toDegrees(azimuth.toDouble()).toFloat()
    
    // Needle angle: -Azimuth (counter-rotates). 
    // If we face North (0), Needle is 0 (Up). 
    // If we face East (90), Needle should contain to point North (Left relative to phone top). -90.
    val needleAngle = -azimuthDeg

    Canvas(modifier = modifier) {
        val center = center
        val radius = size.minDimension / 2
        
        // Dial Background
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.1f),
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawCircle(
            color = Color.Black,
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )

        // NSEW Labels
        val cardinals = listOf("N", "E", "S", "W")
        val cardAngles = listOf(0f, 90f, 180f, 270f) // Standard clockwise from North
        // Draw these fixed on the dial shell
        
        cardAngles.forEachIndexed { index, angle ->
            val rad = (angle - 90f) * (PI / 180f).toFloat()
            val textLayoutResult = textMeasurer.measure(
                text = cardinals[index],
                style = androidx.compose.ui.text.TextStyle(
                    color = if(index == 0) Color.Red else Color.Black,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            )
            val labelPos = Offset(
                center.x + (radius * 0.85f) * cos(rad) - (textLayoutResult.size.width / 2),
                center.y + (radius * 0.85f) * sin(rad) - (textLayoutResult.size.height / 2)
            )
            drawText(textLayoutResult, topLeft = labelPos)
        }
        
        // Ticks
        for (i in 0 until 360 step 30) {
             val rad = (i - 90f) * (PI / 180f).toFloat()
             val start = Offset(
                 center.x + radius * 0.9f * cos(rad),
                 center.y + radius * 0.9f * sin(rad)
             )
             val end = Offset(
                 center.x + radius * cos(rad),
                 center.y + radius * sin(rad)
             )
             drawLine(Color.Gray, start, end, strokeWidth = 1.dp.toPx())
        }

        // Needle
        rotate(degrees = needleAngle, pivot = center) {
            // N arm
            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(center.x, center.y - radius * 0.7f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            // S arm
            drawLine(
                color = Color.Gray,
                start = center,
                end = Offset(center.x, center.y + radius * 0.7f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        drawCircle(Color.Black, radius = 6.dp.toPx())
    }
}

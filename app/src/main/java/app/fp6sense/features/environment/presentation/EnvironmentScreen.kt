package app.fp6sense.features.environment.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun EnvironmentScreen(
    viewModel: EnvironmentViewModel
) {
    val data by viewModel.environmentState.collectAsState()
    var showRawData by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Environment",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Raw Data")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = showRawData,
                onCheckedChange = { showRawData = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Barometer
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Barometer", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (data.pressureHpa != null) {
                    AnalogBarometer(
                        pressure = data.pressureHpa!!,
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text("No Barometer Sensor")
                }

                if (showRawData) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "RAW: ${data.pressureHpa} hPa",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Light
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Light (Lux)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                if (data.illuminanceLux != null) {
                    LightBulbVisualizer(
                        lux = data.illuminanceLux!!,
                        modifier = Modifier.size(120.dp)
                    )
                } else {
                    Text("No Light Sensor")
                }

                if (showRawData) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "RAW: ${data.illuminanceLux} lux",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun AnalogBarometer(pressure: Float, modifier: Modifier = Modifier) {
    // Center at 1000 hPa (1.0 Bar). Range +/- 500 hPa.
    val centerP = 1000f
    val rangeP = 500f // 500 to 1500
    val maxAngle = 135f
    
    // Scaling constant for atan (controls center expansion).
    // Lower val = more expansion at center.
    // 150 gives decent distribution where 100hPa delta is significant.
    val scaleHpa = 150f 
    
    fun getAngle(p: Float): Float {
        val delta = p - centerP
        // Normalize delta to range limit for calc
        val clampedDelta = delta.coerceIn(-rangeP, rangeP)
        val norm = kotlin.math.atan(clampedDelta / scaleHpa).toFloat()
        val maxNorm = kotlin.math.atan(rangeP / scaleHpa).toFloat()
        return (norm / maxNorm) * maxAngle
    }

    val angle = getAngle(pressure)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val center = center
        val radius = size.minDimension / 2
        
        // Dial Background
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.2f),
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawCircle(
            color = Color.Gray,
            radius = radius,
            style = Stroke(width = 4.dp.toPx())
        )

        // Labels
        // We want to show 0.5, 0.9, 1.0, 1.1, 1.5 to highlight the expanded center area
        val labels = listOf(500f, 950f, 1000f, 1050f, 1500f)
        val labelTexts = listOf("0.5", "0.95", "1.0", "1.05", "1.5")
        
        labels.forEachIndexed { index, pVal ->
            val angleDeg = getAngle(pVal)
            val rad = (angleDeg - 90f) * (PI / 180f).toFloat() // -90 offset because 0 is usually 3 o'clock, we treat 0 as 12 o'clock relative to calc?
            // Wait, previous code used: -135 to +135.
            // 0 deg was UP? No, usually 0 is Right.
            // Let's check logic:
            // -135 deg: Top Left. 
            // 0 deg: Top (12 o clock)? Or Right?
            // In standard trig, 0 is Right. -90 is Top.
            // My previous code: `start = center + radius * sin(rad)`, `y - radius * cos(rad)`.
            // This implies `rad=0` -> sin=0, cos=1 -> `y - r` -> TOP.
            // So my internal logic treats 0 radians as UP (12 o'clock).
            // `getAngle` returns -135 to +135.
            // So:
            
            val radForDraw = angleDeg * (PI / 180f).toFloat()

            // Major tick
            val tickStart = Offset(
                center.x + radius * 0.8f * sin(radForDraw),
                center.y - radius * 0.8f * cos(radForDraw)
            )
            val tickEnd = Offset(
                center.x + radius * 0.95f * sin(radForDraw),
                center.y - radius * 0.95f * cos(radForDraw)
            )
            drawLine(
                color = if (pVal == 1000f) Color.Black else Color.DarkGray,
                start = tickStart,
                end = tickEnd,
                strokeWidth = if (pVal == 1000f) 4.dp.toPx() else 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Label
            val textLayoutResult = textMeasurer.measure(
                text = labelTexts[index],
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.Black,
                    fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                    fontWeight = if (pVal == 1000f) androidx.compose.ui.text.font.FontWeight.Bold else null
                )
            )
            
            val labelRadius = radius * 0.65f
            val labelPos = Offset(
                center.x + labelRadius * sin(radForDraw) - (textLayoutResult.size.width / 2),
                center.y - labelRadius * cos(radForDraw) - (textLayoutResult.size.height / 2)
            )
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = labelPos
            )
        }

        // Minor ticks dynamic
        // Lets draw ticks every 10 hPa from 900 to 1100, and every 50 hPa elsewhere?
        // Simpler: Just iterate angles.
        // Or iterate pressure:
        // 500 to 1500, step 25 hPa.
        for (pTick in 500..1500 step 25) {
             val angleDeg = getAngle(pTick.toFloat())
             val radForDraw = angleDeg * (PI / 180f).toFloat()
             
             // Check if it's close to a major label to avoid clash? 
             // Simple logic: Draw short ticks for all, overwrite with majors later (handled above order).
             // Actually, resizing helps.
             
             val startRad = radius * 0.9f
             val endRad = radius * 0.95f
             
             drawLine(
                color = Color.Gray,
                start = Offset(center.x + startRad * sin(radForDraw), center.y - startRad * cos(radForDraw)),
                end = Offset(center.x + endRad * sin(radForDraw), center.y - endRad * cos(radForDraw)),
                strokeWidth = 1.dp.toPx()
             )
        }

        // Needle
        rotate(degrees = angle, pivot = center) {
            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(center.x, center.y - radius * 0.85f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        drawCircle(Color.Black, radius = 6.dp.toPx())
    }
}

@Composable
fun LightBulbVisualizer(lux: Float, modifier: Modifier = Modifier) {
    // Colors: DarkGrey -> Red -> Orange -> Yellow -> White
    val color = when {
        lux < 10f -> lerp(Color.DarkGray, Color.Red, (lux / 10f))
        lux < 100f -> lerp(Color.Red, Color(0xFFFFA500), ((lux - 10f) / 90f)) // Orange
        lux < 1000f -> lerp(Color(0xFFFFA500), Color.Yellow, ((lux - 100f) / 900f))
        else -> lerp(Color.Yellow, Color.White, ((lux - 1000f) / 4000f).coerceAtMost(1f))
    }
    
    val animatedColor by animateColorAsState(targetValue = color, label = "bulbColor")

    Icon(
        imageVector = Icons.Filled.Lightbulb,
        contentDescription = "Light Level",
        modifier = modifier,
        tint = animatedColor
    )
}

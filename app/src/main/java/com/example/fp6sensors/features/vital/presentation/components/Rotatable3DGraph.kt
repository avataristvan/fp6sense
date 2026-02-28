package com.example.fp6sensors.features.vital.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Rotatable3DGraph(
    heartData: List<Float>,
    breathData: List<Float>,
    modifier: Modifier = Modifier,
    pointColor: Color = Color.Green
) {
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotationY += dragAmount.x * 0.5f
                    rotationX += dragAmount.y * 0.5f
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Simple 3D projection logic
        // We'll map:
        // X-axis: Time (index)
        // Y-axis: Heart Signal
        // Z-axis: Breath Signal

        val maxPoints = 300f
        val scaleX = width / maxPoints * 0.8f // Scale time to fit width somewhat
        val scaleY = height / 10f // Scale heart signal vertically
        val scaleZ = height / 4f  // Scale breath signal depth-wise

        val points3D = heartData.zip(breathData).mapIndexed { index, (heart, breath) ->
            Triple(
                (index - heartData.size / 2f) * scaleX * 2f, // Center time around 0
                heart * scaleY,
                breath * scaleZ
            )
        }

        withTransform({
            translate(left = centerX, top = centerY)
            // Apply rotations (Euler angles for simplicity)
            // Note: This is a pseudo-3D rotation on 2D canvas, true 3D matrices would be better but complex to implement from scratch.
            // Rotating around X and Y axes.
        }) {
            val path = Path()

            points3D.forEachIndexed { index, point ->
                val (x, y, z) = point

                // Rotation around X-axis
                val y1 = y * cos(Math.toRadians(rotationX.toDouble())) - z * sin(Math.toRadians(rotationX.toDouble()))
                val z1 = y * sin(Math.toRadians(rotationX.toDouble())) + z * cos(Math.toRadians(rotationX.toDouble()))

                // Rotation around Y-axis
                val x2 = x * cos(Math.toRadians(rotationY.toDouble())) + z1 * sin(Math.toRadians(rotationY.toDouble()))
                // val z2 = -x * sin(Math.toRadians(rotationY.toDouble())) + z1 * cos(Math.toRadians(rotationY.toDouble())) // z2 determines depth

                // 2D Projection (Weak perspective / Orthographic-ish)
                // We'll just plot x2, y1 directly for now, maybe add scale based on z2 for perspective if needed.
                
                val screenX = x2.toFloat()
                val screenY = y1.toFloat()

                if (index == 0) {
                    path.moveTo(screenX, screenY)
                } else {
                    path.lineTo(screenX, screenY)
                }
            }

            drawPath(
                path = path,
                color = pointColor,
                style = Stroke(width = 3f)
            )
            
            // Draw Axes for reference
            // X-Axis (Time) - Red
             drawLine(
                color = Color.Red.copy(alpha = 0.5f),
                start = rotatePoint(-200f, 0f, 0f, rotationX, rotationY),
                end = rotatePoint(200f, 0f, 0f, rotationX, rotationY),
                strokeWidth = 2f
            )
            // Y-Axis (Heart) - Green
            drawLine(
                color = Color.Green.copy(alpha = 0.5f),
                start = rotatePoint(0f, -200f, 0f, rotationX, rotationY),
                end = rotatePoint(0f, 200f, 0f, rotationX, rotationY),
                strokeWidth = 2f
            )
            // Z-Axis (Breath) - Blue
            drawLine(
                color = Color.Blue.copy(alpha = 0.5f),
                start = rotatePoint(0f, 0f, -200f, rotationX, rotationY),
                end = rotatePoint(0f, 0f, 200f, rotationX, rotationY),
                strokeWidth = 2f
            )
        }
    }
}

// Helper to project a single 3D point for axes drawing
fun rotatePoint(x: Float, y: Float, z: Float, rotX: Float, rotY: Float): Offset {
    // Rotation around X-axis
    val y1 = y * cos(Math.toRadians(rotX.toDouble())) - z * sin(Math.toRadians(rotX.toDouble()))
    val z1 = y * sin(Math.toRadians(rotX.toDouble())) + z * cos(Math.toRadians(rotX.toDouble()))

    // Rotation around Y-axis
    val x2 = x * cos(Math.toRadians(rotY.toDouble())) + z1 * sin(Math.toRadians(rotY.toDouble()))
    val y2 = y1 // y is mostly affected by X rotation in this simple model

    return Offset(x2.toFloat(), y2.toFloat())
}

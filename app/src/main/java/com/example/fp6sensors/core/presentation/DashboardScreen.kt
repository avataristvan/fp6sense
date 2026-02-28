package com.example.fp6sensors.core.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val description: String
)

@Composable
fun DashboardScreen(navController: NavController) {
    val items = listOf(
        DashboardItem("GNSS / Location", Icons.Default.LocationOn, "gnss", "GPS, Galileo, Beidou, GLONASS"),
        DashboardItem("Motion", Icons.Default.LocationOn, "motion", "Accelerometer & Gyroscope 3D"),
        DashboardItem("Environment", Icons.Default.LocationOn, "environment", "Barometer & Light"),
        DashboardItem("Magnetic & Compass", Icons.Default.LocationOn, "magnetic", "Hall Sensor Log & 3D Compass"),
        DashboardItem("Proximity", Icons.Default.LocationOn, "proximity", "Camera Overlay & Distance"),
        DashboardItem("Simulated IR", Icons.Default.LocationOn, "infrared", "Simulated IR Spectrum Map"),
        DashboardItem("Vital Signs", Icons.Default.LocationOn, "vital", "Heart Rate (SCG) & Breathing"),
        DashboardItem("Vibration Mic", Icons.Default.LocationOn, "vibration", "Record Sensor Audio (WAV)")
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Fairphone 6 Sensors",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Advanced Sensor Visualization",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            
            // Orientation Control
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            var isLandscape by androidx.compose.runtime.remember { 
                androidx.compose.runtime.mutableStateOf(
                    activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ) 
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Landscape Mode", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Switch(
                    checked = isLandscape,
                    onCheckedChange = { checked ->
                        isLandscape = checked
                        activity?.requestedOrientation = if (checked) {
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(1), // Or 2 for tablet/landscape
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items) { item ->
                    DashboardCard(item) {
                        navController.navigate(item.route)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(item: DashboardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

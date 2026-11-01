package app.fp6sense.features.gnss.presentation

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import app.fp6sense.features.gnss.domain.model.Satellite

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GnssScreen(
    viewModel: GnssViewModel
) {
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        } else {
             viewModel.startLocationUpdates()
        }
    }
    
    // Also start updates when permission changes to granted
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    val locationData by viewModel.locationState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "GNSS Raw Data",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (locationData == null) {
            Text("Waiting for GPS fix...")
        } else {
            locationData?.let { data ->
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                         Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            RawDataItem("Location", "${data.latitude}, ${data.longitude}")
                            RawDataItem("Altitude", "${data.altitude} m")
                            RawDataItem("Accuracy", "${data.accuracy} m")
                            RawDataItem("Speed", "${data.speed} m/s")
                            RawDataItem("Bearing", "${data.bearing}°")
                            RawDataItem("Satellites", "${data.satelliteCount} visible")
                         }
                    }
                    
                    item {
                        Text(
                            "Detailed Satellite Info",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(data.satellites) { satellite ->
                        SatelliteItem(satellite)
                    }
                }
            }
        }
    }
}

@Composable
fun SatelliteItem(satellite: Satellite) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${satellite.constellation} ${satellite.svid}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (satellite.usedInFix) {
                    Text(
                        text = "USED",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Az: ${satellite.azimuthDegrees.toInt()}°", style = MaterialTheme.typography.bodySmall)
                    Text("El: ${satellite.elevationDegrees.toInt()}°", style = MaterialTheme.typography.bodySmall)
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("SNR: ${satellite.cn0DbHz} dBHz", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = (satellite.cn0DbHz / 50f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RawDataItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

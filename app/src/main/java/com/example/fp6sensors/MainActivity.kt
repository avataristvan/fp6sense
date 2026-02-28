package com.example.fp6sensors

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fp6sensors.core.presentation.DashboardScreen
import com.example.fp6sensors.features.environment.data.repository.EnvironmentRepositoryImpl
import com.example.fp6sensors.features.environment.presentation.EnvironmentScreen
import com.example.fp6sensors.features.environment.presentation.EnvironmentViewModel
import com.example.fp6sensors.features.gnss.data.repository.LocationRepositoryImpl
import com.example.fp6sensors.features.gnss.presentation.GnssScreen
import com.example.fp6sensors.features.gnss.presentation.GnssViewModel
import com.example.fp6sensors.features.magnetic.data.repository.MagneticRepositoryImpl
import com.example.fp6sensors.features.magnetic.presentation.MagneticScreen
import com.example.fp6sensors.features.magnetic.presentation.MagneticViewModel
import com.example.fp6sensors.features.motion.data.repository.MotionRepositoryImpl
import com.example.fp6sensors.features.motion.presentation.MotionScreen
import com.example.fp6sensors.features.motion.presentation.MotionViewModel
import com.example.fp6sensors.features.proximity.data.repository.ProximityRepositoryImpl
import com.example.fp6sensors.features.proximity.presentation.ProximityScreen
import com.example.fp6sensors.features.proximity.presentation.ProximityViewModel
import com.example.fp6sensors.features.infrared.presentation.InfraredScreen
import com.example.fp6sensors.features.vibration.data.recorder.SensorWavRecorder
import com.example.fp6sensors.features.vibration.presentation.VibrationScreen
import com.example.fp6sensors.features.vibration.presentation.VibrationViewModel
import com.example.fp6sensors.features.vital.data.repository.VitalRepositoryImpl
import com.example.fp6sensors.features.vital.presentation.VitalScreen
import com.example.fp6sensors.features.vital.presentation.VitalViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Manual DI (Simple for this scope)
        val appContext = applicationContext
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(navController)
                        }
                        
                        composable("gnss") {
                            val viewModel: GnssViewModel = viewModel(
                                factory = ViewModelFactory { GnssViewModel(LocationRepositoryImpl(appContext)) }
                            )
                            GnssScreen(viewModel)
                        }
                        
                        composable("motion") {
                            val viewModel: MotionViewModel = viewModel(
                                factory = ViewModelFactory { MotionViewModel(MotionRepositoryImpl(appContext)) }
                            )
                            MotionScreen(viewModel)
                        }
                        
                        composable("environment") {
                            val viewModel: EnvironmentViewModel = viewModel(
                                factory = ViewModelFactory { EnvironmentViewModel(EnvironmentRepositoryImpl(appContext)) }
                            )
                            EnvironmentScreen(viewModel)
                        }
                        
                        composable("magnetic") {
                            val viewModel: MagneticViewModel = viewModel(
                                factory = ViewModelFactory { MagneticViewModel(MagneticRepositoryImpl(appContext)) }
                            )
                            MagneticScreen(viewModel)
                        }
                        
                        composable("proximity") {
                            val viewModel: ProximityViewModel = viewModel(
                                factory = ViewModelFactory { 
                                    ProximityViewModel(
                                        ProximityRepositoryImpl(appContext),
                                        EnvironmentRepositoryImpl(appContext)
                                    ) 
                                }
                            )
                            ProximityScreen(viewModel)
                        }

                        composable("infrared") {
                            InfraredScreen()
                        }
                        
                        composable("vital") {
                             val viewModel: VitalViewModel = viewModel(
                                factory = ViewModelFactory { VitalViewModel(VitalRepositoryImpl(appContext)) }
                            )
                            VitalScreen(viewModel)
                        }
                        
                        composable("vibration") {
                            val viewModel: VibrationViewModel = viewModel(
                                factory = ViewModelFactory { VibrationViewModel(SensorWavRecorder(appContext)) }
                            )
                            VibrationScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

class ViewModelFactory<T>(val creator: () -> T) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}

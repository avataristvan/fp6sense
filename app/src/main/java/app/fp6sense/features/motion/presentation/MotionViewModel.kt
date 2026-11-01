package app.fp6sense.features.motion.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fp6sense.features.motion.domain.model.MotionData
import app.fp6sense.features.motion.domain.repository.MotionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MotionViewModel(
    private val repository: MotionRepository
) : ViewModel() {

    private val _motionState = MutableStateFlow(MotionData(0f, 0f, 0f))
    val motionState: StateFlow<MotionData> = _motionState.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.isSensorAvailable()) {
                repository.getSensorData().collect {
                    _motionState.value = it
                }
            }
        }
    }
}

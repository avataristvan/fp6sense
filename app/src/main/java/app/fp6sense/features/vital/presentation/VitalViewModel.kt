package app.fp6sense.features.vital.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fp6sense.features.vital.domain.model.VitalData
import app.fp6sense.features.vital.domain.repository.VitalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VitalViewModel(
    private val repository: VitalRepository
) : ViewModel() {

    private val _vitalState = MutableStateFlow(VitalData())
    val vitalState: StateFlow<VitalData> = _vitalState.asStateFlow()

    private val _isMeasuring = MutableStateFlow(true)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.isSensorAvailable()) {
                repository.getSensorData().collect { data ->
                    if (_isMeasuring.value) {
                        _vitalState.value = data
                    }
                }
            }
        }
    }

    fun toggleMeasuring() {
        _isMeasuring.value = !_isMeasuring.value
    }
}

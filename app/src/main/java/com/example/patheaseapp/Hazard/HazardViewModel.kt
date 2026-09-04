package com.example.patheaseapp.Hazard

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.patheaseapp.sharedata.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModelProvider
import android.net.Uri

// Controller/ViewModel for the Hazard/Barrier & Safety feature — holds all state and business logic.
class HazardViewModel(
    private val repository: HazardRepository
) : ViewModel() {

    private val _hazards = MutableStateFlow<List<Hazard>>(emptyList())
    val hazards: StateFlow<List<Hazard>> = _hazards.asStateFlow()

    private val _nearbyWarning = MutableStateFlow<Hazard?>(null)
    val nearbyWarning: StateFlow<Hazard?> = _nearbyWarning.asStateFlow()

    private val _isStillTooLong = MutableStateFlow(false)
    val isStillTooLong: StateFlow<Boolean> = _isStillTooLong.asStateFlow()

    private val _sosTriggered = MutableStateFlow(false)
    val sosTriggered: StateFlow<Boolean> = _sosTriggered.asStateFlow()

    val emergencyContact: StateFlow<String> = UserProfileRepository.emergencyContact

    init {
        viewModelScope.launch {
            try {
                _hazards.value = repository.getHazards()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HazardViewModel", "Failed to load hazards", e)
            }
        }
    }

    fun onBumpDetected(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                repository.addHazard(
                    type = "Bumpy Road",
                    lat = lat,
                    lng = lng,
                    photoUrl = null,
                    reportedBy = "Auto-detected"
                )
                _hazards.value = repository.getHazards()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HazardViewModel", "Failed to report bump", e)
            }
        }
    }
    fun confirmHazardRemoved(hazard: Hazard) {
        viewModelScope.launch {
            try {
                repository.confirmRemoved(hazard.id, hazard.confirmedRemovedCount)
                _hazards.value = repository.getHazards()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HazardViewModel", "Failed to confirm removal", e)
            }
        }
    }

    fun onLocationUpdate(userLat: Double, userLng: Double) {
        viewModelScope.launch {
            val updated = _hazards.value.map { h ->
                h.copy(distanceMeters = distanceBetween(userLat, userLng, h.lat, h.lng))
            }
            _hazards.value = updated
            _nearbyWarning.value = updated.firstOrNull { it.distanceMeters <= 50f }
        }
    }
    fun reportHazard(type: String, lat: Double, lng: Double, photo: android.graphics.Bitmap?) {
        viewModelScope.launch {
            try {
                val photoUrl = photo?.let { bitmap ->
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                    repository.uploadPhoto(stream.toByteArray())
                }
                repository.addHazard(
                    type = type,
                    lat = lat,
                    lng = lng,
                    photoUrl = photoUrl,
                    reportedBy = "Anonymous"
                )
                _hazards.value = repository.getHazards()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HazardViewModel", "Failed to submit report", e)
            }
        }
    }

    fun onStillnessTimeout() {
        _isStillTooLong.value = true
    }

    fun dismissStillnessCheck() {
        _isStillTooLong.value = false
    }

    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    fun triggerSOS() {
        _sosTriggered.value = true
        _isStillTooLong.value = false // close the check-in dialog once SOS is triggered
    }

    fun onSosHandled() {
        _sosTriggered.value = false // reset after the call intent has been launched
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HazardViewModel(HazardRepository(hazardSupabaseClient)) as T
            }
        }
    }
}
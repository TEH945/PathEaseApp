package com.example.patheaseapp.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.patheaseapp.utils.SpeechRecognizerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.google.android.gms.maps.model.LatLng

data class SelectedPlace(
    val name: String,
    val address: String,
    val latLng: LatLng
)

data class RouteInstruction(
    val title: String,
    val distance: String,
    val isHazardAhead: Boolean = false,
    val hazardMessage: String = ""
)

data class HomeUiState(
    val searchQuery: String = "",
    val isListening: Boolean = false,
    val isNavigating: Boolean = false,
    val currentLocationName: String = "Current Location",
    val destinationName: String = "",
    val selectedPlace: SelectedPlace? = null,
    val activeInstruction: RouteInstruction? = null,
    val speechError: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var speechRecognizerManager: SpeechRecognizerManager? = null

    fun initSpeechRecognizer(context: Context) {
        if (speechRecognizerManager == null) {
            speechRecognizerManager = SpeechRecognizerManager(
                context = context,
                onResult = { recognizedText ->
                    _uiState.value = _uiState.value.copy(
                        searchQuery = recognizedText,
                        isListening = false
                    )
                    startNavigationTo(recognizedText)
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        isListening = false,
                        speechError = error
                    )
                }
            )
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(searchQuery = newQuery)
    }

    fun selectPlace(place: SelectedPlace?) {
        _uiState.value = _uiState.value.copy(selectedPlace = place)
    }

    fun startVoiceInput() {
        _uiState.value = _uiState.value.copy(isListening = true, speechError = null)
        speechRecognizerManager?.startListening()
    }

    fun stopVoiceInput() {
        _uiState.value = _uiState.value.copy(isListening = false)
        speechRecognizerManager?.stopListening()
    }

    fun startNavigationTo(destination: String) {
        if (destination.isBlank()) return
        _uiState.value = _uiState.value.copy(
            isNavigating = true,
            destinationName = destination,
            searchQuery = destination,
            activeInstruction = RouteInstruction(
                title = "Head North towards $destination",
                distance = "150m",
                isHazardAhead = true,
                hazardMessage = "Bumpy Road surface ahead (20m)"
            )
        )
    }

    fun clearNavigation() {
        _uiState.value = _uiState.value.copy(
            isNavigating = false,
            destinationName = "",
            searchQuery = "",
            activeInstruction = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.destroy()
    }
}
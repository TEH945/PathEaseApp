package com.example.patheaseapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.patheaseapp.data.local.AccessibilityRepository
import com.example.patheaseapp.data.remote.RouteHistoryItem
import com.example.patheaseapp.data.remote.SupabaseProfile
import com.example.patheaseapp.data.remote.SupabaseStartedLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val supabaseClient: SupabaseClient,
    private val accessibilityRepo: AccessibilityRepository
) : ViewModel() {

    val accessibilitySettings = accessibilityRepo.accessibilityState

    private val _userProfile = MutableStateFlow<SupabaseProfile?>(null)
    val userProfile: StateFlow<SupabaseProfile?> = _userProfile.asStateFlow()

    private val _starredLocations = MutableStateFlow<List<SupabaseStartedLocation>>(emptyList())
    val starredLocations: StateFlow<List<SupabaseStartedLocation>> = _starredLocations.asStateFlow()

    private val _routeHistory = MutableStateFlow<List<RouteHistoryItem>>(emptyList())
    val routeHistory: StateFlow<List<RouteHistoryItem>> = _routeHistory.asStateFlow()

    // User Profile Functions
    fun fetchProfile(userId: String) {
        viewModelScope.launch {
            try {
                val profile = supabaseClient.postgrest["profiles"]
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<SupabaseProfile>()
                _userProfile.value = profile
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProfile(userId: String, newName: String, newEmail: String) {
        viewModelScope.launch {
            try {
                val updated = SupabaseProfile(id = userId, name = newName, email = newEmail)
                supabaseClient.postgrest["profiles"].update(updated) {
                    filter { eq("id", userId) }
                }
                _userProfile.value = updated
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        _userProfile.value = null
    }

    // Starred Locations Functions
    fun fetchStarredLocations(userId: String) {
        viewModelScope.launch {
            try {
                val locations = supabaseClient.postgrest["starred_locations"]
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<SupabaseStartedLocation>()
                _starredLocations.value = locations
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteStarredLocation(userId: String, locationId: String) {
        viewModelScope.launch {
            try {
                supabaseClient.postgrest["starred_locations"].delete {
                    filter { eq("id", locationId) }
                }
                fetchStarredLocations(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Route History Functions
    fun clearHistory() {
        _routeHistory.value = emptyList()
    }

    // Accessibility Toggle Functions
    fun toggleVisualAssistance(enabled: Boolean) {
        accessibilityRepo.updatePreferences { it.copy(visualAssistanceMode = enabled) }
    }

    fun toggleVoiceGuidance(enabled: Boolean) {
        accessibilityRepo.updatePreferences { it.copy(voiceGuidanceEnabled = enabled) }
    }

    fun toggleWheelchairAccess(enabled: Boolean) {
        accessibilityRepo.updatePreferences {
            it.copy(
                wheelchairAccessEnabled = enabled,
                strollerModeEnabled = if (enabled) false else it.strollerModeEnabled
            )
        }
    }

    fun toggleStrollerMode(enabled: Boolean) {
        accessibilityRepo.updatePreferences {
            it.copy(
                strollerModeEnabled = enabled,
                wheelchairAccessEnabled = if (enabled) false else it.wheelchairAccessEnabled
            )
        }
    }
}
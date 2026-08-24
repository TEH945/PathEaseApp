package com.example.patheaseapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.patheaseapp.data.local.AccessibilityRepository
import com.example.patheaseapp.data.remote.RouteHistoryItem
import com.example.patheaseapp.data.remote.SupabaseProfile
import com.example.patheaseapp.data.remote.SupabaseStartedLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val supabaseClient: SupabaseClient,
    private val accessibilityRepo: AccessibilityRepository,
) : ViewModel() {

    val accessibilitySettings = accessibilityRepo.accessibilityState

    private val _userProfile = MutableStateFlow<SupabaseProfile?>(null)
    val userProfile: StateFlow<SupabaseProfile?> = _userProfile.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    private val _starredLocations = MutableStateFlow<List<SupabaseStartedLocation>>(emptyList())
    val starredLocations: StateFlow<List<SupabaseStartedLocation>> = _starredLocations.asStateFlow()

    private val _routeHistory = MutableStateFlow<List<RouteHistoryItem>>(emptyList())
    val routeHistory: StateFlow<List<RouteHistoryItem>> = _routeHistory.asStateFlow()

    // User Profile Functions
    fun fetchProfile(userId: String) {
        viewModelScope.launch {
            _isProfileLoading.value = true
            _profileError.value = null
            try {
                // Try to fetch existing profile
                val profile = supabaseClient.postgrest["profiles"]
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<SupabaseProfile>()
                _userProfile.value = profile
            } catch (e: Exception) {
                // If not found, pre-fill with data from Auth session
                val currentUser = supabaseClient.auth.currentUserOrNull()
                if (currentUser != null) {
                    // Try to get the name from various possible metadata keys
                    val metadata = currentUser.userMetadata
                    val authName = metadata?.get("name")?.jsonPrimitive?.contentOrNull
                        ?: metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                        ?: metadata?.get("display_name")?.jsonPrimitive?.contentOrNull
                        ?: currentUser.email?.substringBefore("@") // Fallback to email username
                        ?: ""
                    
                    val authEmail = currentUser.email ?: ""
                    
                    val dummyProfile = SupabaseProfile(
                        id = userId,
                        name = authName,
                        email = authEmail,
                        emergencyContact = "999"
                    )
                    _userProfile.value = dummyProfile
                } else {
                    _profileError.value = "User session not found."
                }
                e.printStackTrace()
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    fun updateProfile(userId: String, newName: String, newEmail: String, newPhone: String) {
        viewModelScope.launch {
            _isProfileLoading.value = true
            _profileError.value = null
            try {
                val updated = SupabaseProfile(
                    id = userId,
                    name = newName,
                    email = newEmail,
                    emergencyContact = newPhone,
                )
                supabaseClient.postgrest["profiles"].upsert(updated)
                _userProfile.value = updated
            } catch (e: Exception) {
                _profileError.value = "Failed to save profile: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                supabaseClient.auth.signOut()
                _userProfile.value = null
                _starredLocations.value = emptyList()
                _routeHistory.value = emptyList()
            } catch (e: Exception) {
                // If network sign out fails, force clear local state to allow login screen to show
                _userProfile.value = null 
                e.printStackTrace()
            }
        }
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
                strollerModeEnabled = if (enabled) false else it.strollerModeEnabled,
            )
        }
    }

    fun toggleStrollerMode(enabled: Boolean) {
        accessibilityRepo.updatePreferences {
            it.copy(
                strollerModeEnabled = enabled,
                wheelchairAccessEnabled = if (enabled) false else it.wheelchairAccessEnabled,
            )
        }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        accessibilityRepo.updatePreferences { it.copy(keepScreenOn = enabled) }
    }
}
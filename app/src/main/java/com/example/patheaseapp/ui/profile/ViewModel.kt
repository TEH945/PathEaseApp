package com.example.patheaseapp.ui.profile

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.patheaseapp.data.local.AccessibilityRepository
import com.example.patheaseapp.data.remote.RouteHistoryItem
import com.example.patheaseapp.data.remote.SupabaseProfile
import com.example.patheaseapp.data.remote.SupabaseStartedLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import android.content.Context
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ProfileViewModel(
    private val supabaseClient: SupabaseClient,
    private val accessibilityRepo: AccessibilityRepository,
) : ViewModel() {

    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    val accessibilitySettings = accessibilityRepo.accessibilityState

    private val _userProfile = MutableStateFlow<SupabaseProfile?>(null)
    val userProfile: StateFlow<SupabaseProfile?> = _userProfile.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(value = false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    private val _starredLocations = MutableStateFlow<List<SupabaseStartedLocation>>(emptyList())
    val starredLocations: StateFlow<List<SupabaseStartedLocation>> = _starredLocations.asStateFlow()

    private val _routeHistory = MutableStateFlow<List<RouteHistoryItem>>(emptyList())
    val routeHistory: StateFlow<List<RouteHistoryItem>> = _routeHistory.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

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
                        emergencyContact = "999",
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
                _profileError.value = e.message ?: "An unexpected error occurred"
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

    fun addStarredLocation(userId: String, name: String, address: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Adding starred location: $name for user: $userId")
                if (userId.isBlank()) {
                    _profileError.value = "User not logged in."
                    return@launch
                }
                val newLocation = SupabaseStartedLocation(
                    userId = userId,
                    name = name,
                    address = address,
                    latitude = lat,
                    longitude = lng,
                )
                supabaseClient.postgrest["starred_locations"].insert(newLocation)
                Log.d("ProfileViewModel", "Successfully added starred location")
                fetchStarredLocations(userId)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error adding starred location: ${e.message}", e)
                _profileError.value = "Failed to save: ${e.message}"
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
    fun fetchRouteHistory(userId: String) {
        viewModelScope.launch {
            Log.d("HistoryDebug", "fetchRouteHistory called with userId: '$userId'")
            if (userId.isBlank()) {
                Log.e("HistoryDebug", "fetchRouteHistory: userId is blank!")
                appContext?.let { Toast.makeText(it, "Error: User not identified", Toast.LENGTH_SHORT).show() }
                return@launch
            }
            _isHistoryLoading.value = true
            _historyError.value = null
            try {
                Log.d("HistoryDebug", "Fetching from postgrest...")
                val history = supabaseClient.postgrest["route_history"]
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<RouteHistoryItem>()
                android.util.Log.d("HistoryDebug", "Fetched ${history.size} rows for userId=$userId")
                _routeHistory.value = history.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e("HistoryDebug", "Fetch failed: ${e.message}", e)
                _historyError.value = "Load failed: ${e.message}"
            } finally {
                _isHistoryLoading.value = false
            }
        }
    }

    fun addRouteHistoryItem(userId: String, origin: String, destination: String, destLat: Double? = null, destLng: Double? = null) {
        viewModelScope.launch {
            android.util.Log.d("HistoryDebug", "addRouteHistoryItem called for dest: $destination, userId: '$userId'")
            if (userId.isBlank()) {
                Log.e("HistoryDebug", "addRouteHistoryItem: userId is blank!")
                return@launch
            }
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")

                val newItem = RouteHistoryItem(
                    userId = userId,
                    origin = origin,
                    destination = destination,
                    timestamp = sdf.format(Date()),
                    destinationLatitude = destLat,
                    destinationLongitude = destLng
                )
                android.util.Log.d("HistoryDebug", "Inserting item into route_history...")
                supabaseClient.postgrest["route_history"].insert(newItem)
                Log.d("HistoryDebug", "Insert succeeded")
                fetchRouteHistory(userId)
            } catch (e: Exception) {
                Log.e("HistoryDebug", "Insert failed: ${e.message}", e)
                val detailedError = e.message ?: "Unknown database error"
                _historyError.value = "Save failed: $detailedError"
                appContext?.let {
                    Toast.makeText(it, "Database Error: $detailedError", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun clearHistory(userId: String) {
        viewModelScope.launch {
            try {
                supabaseClient.postgrest["route_history"].delete {
                    filter { eq("user_id", userId) }
                }
                _routeHistory.value = emptyList()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error clearing history: ${e.message}", e)
            }
        }
    }

    fun toggleBlindMode(enabled: Boolean) {
        accessibilityRepo.updatePreferences { it.copy(blindModeEnabled = enabled) }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        accessibilityRepo.updatePreferences { it.copy(keepScreenOn = enabled) }
    }
}

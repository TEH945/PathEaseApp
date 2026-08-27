package com.example.patheaseapp.ui.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patheaseapp.ui.profile.ProfileViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import com.google.android.libraries.places.widget.AutocompleteActivity
import android.util.Log
import androidx.compose.runtime.collectAsState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    profileViewModel: ProfileViewModel,
    userId: String
) {
    val kualaLumpur = LatLng(3.1390, 101.6869)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kualaLumpur, 12f)
    }
    val coroutineScope = rememberCoroutineScope()
    
    // Track selected place for starring and history
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    val starredLocations by profileViewModel.starredLocations.collectAsState()
    val isStarred = selectedPlace?.let { place ->
        starredLocations.any { it.latitude == place.latLng?.latitude && it.longitude == place.latLng?.longitude }
    } ?: false

    Box(modifier = modifier.fillMaxSize()) {
        // Map View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            selectedPlace?.latLng?.let { latLng ->
                Marker(
                    state = MarkerState(position = latLng),
                    title = selectedPlace?.name ?: "Selected Location",
                    snippet = selectedPlace?.address
                )
            }
        }

        // Floating Search Bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            MapSearchBar(
                onPlaceSelected = { place ->
                    selectedPlace = place
                    place.latLng?.let { latLng ->
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                            )
                        }
                        // Add to History
                        profileViewModel.addRouteHistoryItem(
                            userId = userId,
                            origin = "Current Location",
                            destination = place.name ?: place.address ?: "Unknown Location"
                        )
                    }
                }
            )
        }

        // Selected Place Info Card (for Starring)
        selectedPlace?.let { place ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.name ?: "Unknown Name",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = place.address ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isStarred) {
                                // Find and delete (simplified for this implementation)
                                val locToDelete = starredLocations.find { 
                                    it.latitude == place.latLng?.latitude && it.longitude == place.latLng?.longitude 
                                }
                                locToDelete?.id?.let { profileViewModel.deleteStarredLocation(userId, it) }
                            } else {
                                profileViewModel.addStarredLocation(
                                    userId = userId,
                                    name = place.name ?: "Saved Location",
                                    address = place.address ?: "",
                                    lat = place.latLng?.latitude ?: 0.0,
                                    lng = place.latLng?.longitude ?: 0.0
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isStarred) "Unstar location" else "Star location",
                            tint = if (isStarred) Color(0xFFFFD700) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapSearchBar(
    onPlaceSelected: (Place) -> Unit
) {
    val context = LocalContext.current
    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { data ->
                    val place = Autocomplete.getPlaceFromIntent(data)
                    onPlaceSelected(place)
                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                result.data?.let { data ->
                    val status = Autocomplete.getStatusFromIntent(data)
                    Log.e("MapSearchBar", "Places error: ${status.statusMessage}")
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d("MapSearchBar", "User canceled search")
            }
        }
    }

    Button(
        onClick = {
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                fields
            ).build(context)
            launcher.launch(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("Search Google Maps...")
    }
}
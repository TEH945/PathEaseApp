package com.example.patheaseapp.ui.home

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.patheaseapp.ui.profile.ProfileViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    userId: String,
    modifier: Modifier = Modifier,
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val starredLocations by profileViewModel.starredLocations.collectAsState()
    val profileError by profileViewModel.profileError.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        Log.d("HomeScreen", "Fetching starred locations for user: $userId")
        profileViewModel.fetchStarredLocations(userId)
    }

    LaunchedEffect(profileError) {
        profileError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val kualaLumpur = LatLng(3.1390, 101.6869)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kualaLumpur, 12f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Map View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            uiState.selectedPlace?.let { place ->
                Marker(
                    state = MarkerState(position = place.latLng),
                    title = place.name,
                    snippet = place.address
                )
            }
        }

        // Floating Search Bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            MapSearchBar { place ->
                val latLng = place.latLng ?: return@MapSearchBar
                homeViewModel.selectPlace(
                    SelectedPlace(
                        name = place.name ?: "Unknown",
                        address = place.address ?: "No address",
                        latLng = latLng
                    )
                )
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                    )
                }
            }
        }

        // Place Detail Card
        uiState.selectedPlace?.let { place ->
            val isStarred = starredLocations.any { 
                Math.abs(it.latitude - place.latLng.latitude) < 0.0001 && 
                Math.abs(it.longitude - place.latLng.longitude) < 0.0001 
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = place.name, style = MaterialTheme.typography.titleLarge)
                            Text(text = place.address, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { homeViewModel.selectPlace(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        OutlinedButton(
                            onClick = {
                                Log.d("HomeScreen", "Save button clicked for: ${place.name}")
                                if (!isStarred) {
                                    profileViewModel.addStarredLocation(
                                        userId = userId,
                                        name = place.name,
                                        address = place.address,
                                        lat = place.latLng.latitude,
                                        lng = place.latLng.longitude
                                    )
                                } else {
                                    Log.d("HomeScreen", "Place is already starred")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isStarred) "Saved" else "Save")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                profileViewModel.addRouteHistoryItem(
                                    userId = userId,
                                    origin = uiState.currentLocationName,
                                    destination = place.name
                                )
                                homeViewModel.startNavigationTo(place.name)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Directions")
                        }
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
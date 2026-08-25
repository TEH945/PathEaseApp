package com.example.patheaseapp.ui.home

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patheaseapp.Hazard.StartLocationUpdates
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
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    // Fallback shown only until the real GPS fix comes in
    val kualaLumpur = LatLng(3.1390, 101.6869)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kualaLumpur, 12f)
    }
    val coroutineScope = rememberCoroutineScope()

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // Streams real GPS updates; recenters the map on the first fix only
    // (so the camera doesn't keep snapping back while the user pans around later)
    StartLocationUpdates { lat, lng ->
        val latLng = LatLng(lat, lng)
        userLocation = latLng
        if (!hasCenteredOnUser) {
            hasCenteredOnUser = true
            coroutineScope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Map View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "You are here"
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
                onPlaceSelected = { selectedLatLng ->
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(selectedLatLng, 16f)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun MapSearchBar(
    onPlaceSelected: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { data ->
                    val place = Autocomplete.getPlaceFromIntent(data)
                    place.latLng?.let { latLng -> onPlaceSelected(latLng) }
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
package com.example.patheaseapp.ui.home

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patheaseapp.Hazard.StartLocationUpdates
import com.example.patheaseapp.ui.profile.ProfileViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private enum class SafetyLevel {
    SAFE, CAUTION, DANGER
}

private data class HazardPoint(
    val location: LatLng,
    val level: SafetyLevel,
    val radiusMeters: Double,
)

private data class RouteStep(
    val points: List<LatLng>,
    val instruction: String,
)

private data class WalkingRoute(
    val points: List<LatLng>,
    val distanceMeters: Int,
    val durationSeconds: Long,
    val steps: List<RouteStep>,
)

private val hazardPoints = listOf(
    HazardPoint(
        location = LatLng(3.1395, 101.6875),
        level = SafetyLevel.CAUTION,
        radiusMeters = 50.0,
    ),
    HazardPoint(
        location = LatLng(3.1400, 101.6880),
        level = SafetyLevel.DANGER,
        radiusMeters = 40.0,
    ),
)

private fun calculateDistanceMeters(a: LatLng, b: LatLng): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)

    val x = (sin(dLat / 2).pow(2)) +
            (cos(lat1) * cos(lat2) * (sin(dLon / 2).pow(2)))

    return earthRadius * 2 * atan2(sqrt(x), sqrt(1 - x))
}

private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        var shift = 0
        var result = 0

        while (true) {
            val b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
            if (b < 0x20) break
        }

        val dLat = if ((result and 1) != 0) {
            (result shr 1).inv()
        } else {
            result shr 1
        }

        lat += dLat

        shift = 0
        result = 0

        while (true) {
            val b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
            if (b < 0x20) break
        }

        val dLng = if ((result and 1) != 0) {
            (result shr 1).inv()
        } else {
            result shr 1
        }

        lng += dLng

        poly.add(
            LatLng(
                lat / 100000.0,
                lng / 100000.0,
            ),
        )
    }

    return poly
}

private suspend fun getWalkingRoute(
    origin: LatLng,
    destination: LatLng,
    apiKey: String,
): WalkingRoute? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://routes.googleapis.com/directions/v2:computeRoutes")

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("X-Goog-Api-Key", apiKey)
        connection.setRequestProperty(
            "X-Goog-FieldMask",
            "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs.steps.polyline.encodedPolyline,routes.legs.steps.navigationInstruction.instructions",
        )
        connection.doOutput = true

        val requestBody = """
            {
              "origin": {
                "location": {
                  "latLng": {
                    "latitude": ${origin.latitude},
                    "longitude": ${origin.longitude}
                  }
                }
              },
              "destination": {
                "location": {
                  "latLng": {
                    "latitude": ${destination.latitude},
                    "longitude": ${destination.longitude}
                  }
                }
              },
              "travelMode": "WALK",
              "computeAlternativeRoutes": false,
              "polylineQuality": "HIGH_QUALITY",
              "languageCode": "en",
              "units": "METRIC"
            }
        """.trimIndent()

        connection.outputStream.use {
            it.write(requestBody.toByteArray())
        }

        val responseCode = connection.responseCode
        if (responseCode !in (200..299)) {
            Log.e("PathEaseRoute", "Routes API error: $responseCode")
            return@withContext null
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val routes = json.optJSONArray("routes")

        if (routes == null || routes.length() == 0) return@withContext null

        val route = routes.getJSONObject(0)
        val distanceMeters = route.optInt("distanceMeters", 0)
        val durationText = route.optString("duration", "0s")
        val durationSeconds = durationText.removeSuffix("s").toDoubleOrNull()?.toLong() ?: 0L
        val routePolyline = route.getJSONObject("polyline").getString("encodedPolyline")
        val routePoints = decodePolyline(routePolyline)

        val steps = mutableListOf<RouteStep>()
        val legs = route.optJSONArray("legs")

        if (legs != null) {
            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                val legSteps = leg.optJSONArray("steps")
                if (legSteps != null) {
                    for (j in 0 until legSteps.length()) {
                        val step = legSteps.getJSONObject(j)
                        val stepPolyline = step.getJSONObject("polyline").getString("encodedPolyline")
                        val instruction = step.optJSONObject("navigationInstruction")
                            ?.optString("instructions", "Continue") ?: "Continue"

                        steps.add(RouteStep(points = decodePolyline(stepPolyline), instruction = instruction))
                    }
                }
            }
        }

        WalkingRoute(
            points = routePoints,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            steps = steps,
        )
    } catch (e: Exception) {
        Log.e("PathEaseRoute", "Route request failed", e)
        null
    }
}

private fun getSafetyLevel(point: LatLng, hazards: List<HazardPoint>): SafetyLevel {
    var level = SafetyLevel.SAFE
    for (hazard in hazards) {
        val distance = calculateDistanceMeters(point, hazard.location)
        if (distance <= hazard.radiusMeters) {
            if (hazard.level == SafetyLevel.DANGER) return SafetyLevel.DANGER
            if (hazard.level == SafetyLevel.CAUTION) level = SafetyLevel.CAUTION
        }
    }
    return level
}

private fun safetyColor(level: SafetyLevel): Color {
    return when (level) {
        SafetyLevel.SAFE -> Color(0xFF22A447)
        SafetyLevel.CAUTION -> Color(0xFFFFB000)
        SafetyLevel.DANGER -> Color(0xFFE53935)
    }
}

private fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
    } else {
        "$meters m"
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = (seconds / 60).toInt()
    return if (minutes < 60) "$minutes min" else "${minutes / 60} hr ${minutes % 60} min"
}

@Composable
fun HomeScreen(
    @Suppress("unused") homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    userId: String,
    modifier: Modifier = Modifier,
) {
    val kualaLumpur = LatLng(3.1390, 101.6869)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kualaLumpur, 12f)
    }
    val coroutineScope = rememberCoroutineScope()
    val starredLocations by profileViewModel.starredLocations.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        profileViewModel.fetchStarredLocations(userId)
    }

    val apiKey = stringResource(id = com.example.patheaseapp.R.string.google_maps_key)
    var userLocation by remember { mutableStateOf<LatLng?>(value = null) }
    var hasCenteredOnUser by remember { mutableStateOf(value = false) }
    var searchedPlace by remember { mutableStateOf<LatLng?>(value = null) }
    var searchedPlaceName by remember { mutableStateOf<String?>(value = null) }
    var searchedPlaceAddress by remember { mutableStateOf<String?>(value = null) }
    var walkingRoute by remember { mutableStateOf<WalkingRoute?>(value = null) }
    var routeStarted by remember { mutableStateOf(value = false) }
    var routeLoading by remember { mutableStateOf(value = false) }
    var routeError by remember { mutableStateOf<String?>(value = null) }
    var currentInstruction by remember { mutableStateOf(value = "Follow the pedestrian route") }

    val isStarred = remember(searchedPlace, starredLocations) {
        starredLocations.any {
            (it.latitude == searchedPlace?.latitude) && (it.longitude == searchedPlace?.longitude)
        }
    }

    StartLocationUpdates { lat: Double, lng: Double ->
        val latLng = LatLng(lat, lng)
        userLocation = latLng

        if (!hasCenteredOnUser) {
            hasCenteredOnUser = true
            coroutineScope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            }
        }

        if (routeStarted && walkingRoute != null) {
            val route = walkingRoute!!
            var closestIndex = 0
            var closestDistance = Double.MAX_VALUE

            route.points.forEachIndexed { index, point ->
                val distance = calculateDistanceMeters(latLng, point)
                if (distance < closestDistance) {
                    closestDistance = distance
                    closestIndex = index
                }
            }

            if (closestIndex < route.points.lastIndex) {
                currentInstruction = findInstructionForPoint(route, closestIndex)
            }

            coroutineScope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = userLocation != null),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false),
        ) {
            searchedPlace?.let { place ->
                Marker(
                    state = MarkerState(position = place),
                    title = searchedPlaceName ?: "Destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                )
            }

            walkingRoute?.let { route ->
                DrawSafetyRoute(points = route.points)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        ) {
            MapSearchBar { selectedLatLng, placeName, placeAddress ->
                searchedPlace = selectedLatLng
                searchedPlaceName = placeName
                searchedPlaceAddress = placeAddress
                walkingRoute = null
                routeStarted = false
                routeError = null
            }
        }

        FloatingActionButton(
            onClick = {
                userLocation?.let { location ->
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location, 17f))
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 88.dp, end = 16.dp)
                .size(48.dp),
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter map",
            )
        }

        if ((searchedPlace != null) && !routeStarted) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = searchedPlaceName ?: "Destination",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(weight = 1f),
                        )

                        IconButton(
                            onClick = {
                                searchedPlace?.let { latLng ->
                                    if (isStarred) {
                                        val star = starredLocations.find {
                                            (it.latitude == latLng.latitude) && (it.longitude == latLng.longitude)
                                        }
                                        star?.id?.let { profileViewModel.deleteStarredLocation(userId, it) }
                                    } else {
                                        profileViewModel.addStarredLocation(
                                            userId = userId,
                                            name = searchedPlaceName ?: "Unnamed Location",
                                            address = searchedPlaceAddress ?: "No address",
                                            lat = latLng.latitude,
                                            lng = latLng.longitude,
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isStarred) "Unstar location" else "Star location",
                                tint = if (isStarred) Color(0xFFFFB000) else Color.Gray,
                            )
                        }
                    }

                    walkingRoute?.let { route ->
                        Text(
                            text = "${formatDistance(route.distanceMeters)} • ${formatDuration(route.durationSeconds)}",
                            modifier = Modifier.padding(top = 4.dp),
                            color = Color.Gray,
                        )
                    } ?: run {
                        Text(
                            text = "Walking route available",
                            modifier = Modifier.padding(top = 4.dp),
                            color = Color.Gray,
                        )
                    }

                    routeError?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier.padding(top = 8.dp),
                            color = Color(0xFFE53935),
                        )
                    }

                    Button(
                        onClick = {
                            val origin = userLocation
                            val destination = searchedPlace

                            if (origin != null && destination != null) {
                                routeLoading = true
                                routeError = null

                                coroutineScope.launch {
                                    // Add to history
                                    profileViewModel.addRouteHistoryItem(
                                        userId = userId,
                                        origin = "Current Location",
                                        destination = searchedPlaceName ?: "Destination",
                                    )

                                    val route = getWalkingRoute(origin, destination, apiKey)
                                    routeLoading = false

                                    if (route != null) {
                                        walkingRoute = route
                                        routeStarted = true
                                        currentInstruction = route.steps.firstOrNull()?.instruction ?: "Follow the pedestrian route"

                                        val boundsBuilder = LatLngBounds.Builder()
                                        route.points.forEach { boundsBuilder.include(it) }

                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 180),
                                        )
                                    } else {
                                        routeError = "Unable to find a walking route."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(size = 14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )

                        Text(
                            text = if (routeLoading) "Finding Safe Route..." else "Start Safe Route",
                        )
                    }
                }
            }
        }

        if (routeStarted && walkingRoute != null) {
            val route = walkingRoute!!
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                ) {
                    Text(
                        text = currentInstruction,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "${formatDistance(route.distanceMeters)} • ${formatDuration(route.durationSeconds)}",
                        modifier = Modifier.padding(top = 4.dp),
                        color = Color.Gray,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        SafetyIndicator(color = Color(0xFF22A447), text = "Safe")
                        SafetyIndicator(color = Color(0xFFFFB000), text = "Caution")
                        SafetyIndicator(color = Color(0xFFE53935), text = "Danger")
                    }

                    Text(
                        text = "Walking route • Check surroundings",
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawSafetyRoute(points: List<LatLng>, hazards: List<HazardPoint> = hazardPoints) {
    if (points.size < 2) return
    for (i in 0 until points.lastIndex) {
        val start = points[i]
        val end = points[i + 1]
        val middle = LatLng((start.latitude + end.latitude) / 2, (start.longitude + end.longitude) / 2)
        val safety = getSafetyLevel(middle, hazards)

        Polyline(points = listOf(start, end), color = safetyColor(safety), width = 13f)
    }
}

private fun findInstructionForPoint(route: WalkingRoute, pointIndex: Int): String {
    if (route.steps.isEmpty()) return "Follow the pedestrian route"
    var currentIndex = 0
    for (step in route.steps) {
        val stepSize = step.points.size
        if (pointIndex <= (currentIndex + stepSize)) return step.instruction
        currentIndex += stepSize
    }
    return route.steps.last().instruction
}

@Composable
fun SafetyIndicator(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(size = 12.dp)
                .background(color = color, shape = CircleShape),
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 5.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun MapSearchBar(onPlaceSelected: (LatLng, String?, String?) -> Unit) {
    val context = LocalContext.current
    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val place = Autocomplete.getPlaceFromIntent(data)
                place.latLng?.let { latLng -> onPlaceSelected(latLng, place.name, place.address) }
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            result.data?.let { data ->
                val status = Autocomplete.getStatusFromIntent(data)
                Log.e("MapSearchBar", "Places error: ${status.statusMessage}")
            }
        }
    }

    Card(
        onClick = {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(context)
            launcher.launch(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(size = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            Text(
                text = "Search here",
                modifier = Modifier.padding(start = 16.dp),
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

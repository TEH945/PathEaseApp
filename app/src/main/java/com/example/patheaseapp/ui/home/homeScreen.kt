package com.example.patheaseapp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.example.patheaseapp.ui.theme.PathEaseAppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patheaseapp.Hazard.WarningBanner
import com.example.patheaseapp.Hazard.ReportHazardScreen
import androidx.compose.material3.FloatingActionButton
import com.example.patheaseapp.Hazard.HazardViewModel
import com.example.patheaseapp.Hazard.StartLocationUpdates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    hazardViewModel: HazardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val nearbyWarning by hazardViewModel.nearbyWarning.collectAsStateWithLifecycle()
    val isStillTooLong by hazardViewModel.isStillTooLong.collectAsStateWithLifecycle()
    val sosTriggered by hazardViewModel.sosTriggered.collectAsStateWithLifecycle()
    val emergencyContact by hazardViewModel.emergencyContact.collectAsStateWithLifecycle()
    
    HomeScreenContent(
        uiState = uiState,
        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onStartNavigation = { viewModel.startNavigationTo(it) },
        onClearNavigation = { viewModel.clearNavigation() },
        hazardViewModel = hazardViewModel,
        nearbyWarning = nearbyWarning,
        isStillTooLong = isStillTooLong,
        sosTriggered = sosTriggered,
        emergencyContact = emergencyContact,
        onSafe = { hazardViewModel.dismissStillnessCheck() },
        onSOS = { hazardViewModel.triggerSOS() },
        onSosHandled = { hazardViewModel.onSosHandled() },
        onReportHazard = { type, lat, lng ->
            hazardViewModel.reportHazard(type, lat, lng, photoUri = null)
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onStartNavigation: (String) -> Unit,
    onClearNavigation: () -> Unit,
    hazardViewModel: HazardViewModel,
    nearbyWarning: com.example.patheaseapp.Hazard.Hazard? = null,
    isStillTooLong: Boolean = false,
    sosTriggered: Boolean = false,
    emergencyContact: String = "999",
    onSafe: () -> Unit = {},
    onSOS: () -> Unit = {},
    onSosHandled: () -> Unit = {},
    onReportHazard: (String, Double, Double) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    StartLocationUpdates { lat, lng ->
        hazardViewModel.onLocationUpdate(lat, lng)
    }
    var showReportHazard by remember { mutableStateOf(false) }

    if (showReportHazard) {
        ReportHazardScreen(
            onSubmit = { type, lat, lng ->
                onReportHazard(type, lat, lng)
                showReportHazard = false
            },
            onCancel = { showReportHazard = false }
        )
        return
    }

    LaunchedEffect(nearbyWarning) {
        nearbyWarning?.let { hazard ->
            com.example.patheaseapp.Hazard.vibrate(context)
            com.example.patheaseapp.Hazard.speakWarning(
                context,
                "${hazard.type} ahead, ${hazard.distanceMeters.toInt()} meters"
            )
        }
    }
    LaunchedEffect(sosTriggered) {
        if (sosTriggered) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:$emergencyContact".toUri()
            }
            context.startActivity(intent)
            onSosHandled()
        }
    }

    // Default map position (Coordinates for center view)
    val defaultLocation = LatLng(3.1390, 101.6869)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Voice recognition launcher (System Intent approach)
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            spokenText?.let { text ->
                onSearchQueryChanged(text)
                onStartNavigation(text)
            }
        }
    }

    // Audio recording permission launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your destination...")
                }
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Voice input not supported on this device's system settings.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Google Map View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = defaultLocation),
                title = uiState.currentLocationName,
                snippet = "Current Location"
            )
        }

        // 2. Top Floating Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { onSearchQueryChanged(it) },
                        placeholder = { Text("Enter a location...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            onStartNavigation(uiState.searchQuery)
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    // Microphone Icon Button
                    IconButton(
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your destination...")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Voice input not supported on this device.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = Color.DarkGray
                        )
                    }

                    // Search Button
                    IconButton(
                        onClick = { onStartNavigation(uiState.searchQuery) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.DarkGray
                        )
                    }
                }
            }
        }

        // 3. Active Navigation Instructions Card
        if (uiState.isNavigating && (uiState.activeInstruction != null)) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.activeInstruction.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = uiState.activeInstruction.distance,
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = { onClearNavigation() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close navigation",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        nearbyWarning?.let {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)) {
                WarningBanner(it)
            }
        }

        FloatingActionButton(
            onClick = { showReportHazard = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Report a hazard")
        }
    }

    // NEW: stillness/SOS dialog
    if (isStillTooLong) {
        StillnessCheckDialog(onSafe = onSafe, onSOS = onSOS)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PathEaseAppTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                currentLocationName = "Kuala Lumpur City Centre",
                isNavigating = true,
                activeInstruction = RouteInstruction(
                    title = "Head North towards KL Tower",
                    distance = "200m"
                )
            ),
            onSearchQueryChanged = {},
            onStartNavigation = {},
            onClearNavigation = {},
            hazardViewModel = HazardViewModel(),
        )
    }
}
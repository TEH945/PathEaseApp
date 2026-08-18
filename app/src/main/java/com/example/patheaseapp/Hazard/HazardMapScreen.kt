package com.example.patheaseapp.Hazard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

// UI screen for the hazard map — observes ViewModel state and renders it, no logic here.
@Composable
fun HazardMapScreen(viewModel: HazardViewModel) {
    val hazards by viewModel.hazards.collectAsStateWithLifecycle()
    val nearbyWarning by viewModel.nearbyWarning.collectAsStateWithLifecycle()
    val isStillTooLong by viewModel.isStillTooLong.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(nearbyWarning) {
        nearbyWarning?.let { hazard ->
            vibrate(context)
            speakWarning(context, "${hazard.type} ahead, ${hazard.distanceMeters.toInt()} meters")
        }
    }

    Box(Modifier.fillMaxSize()) {
        nearbyWarning?.let { WarningBanner(it) }
    }

    if (isStillTooLong) {
        StillnessCheckDialog(
            onSafe = { viewModel.dismissStillnessCheck() },
            onSOS = { /* TODO: trigger emergency contact */ }
        )
    }
}
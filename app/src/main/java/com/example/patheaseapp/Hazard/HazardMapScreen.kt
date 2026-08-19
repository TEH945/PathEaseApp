package com.example.patheaseapp.Hazard

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import com.example.patheaseapp.ui.home.StillnessCheckDialog

// UI screen for the hazard map — observes ViewModel state and renders it, no logic here.
@Composable
fun HazardMapScreen(viewModel: HazardViewModel) {
    val hazards by viewModel.hazards.collectAsStateWithLifecycle()
    val nearbyWarning by viewModel.nearbyWarning.collectAsStateWithLifecycle()
    val isStillTooLong by viewModel.isStillTooLong.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sosTriggered by viewModel.sosTriggered.collectAsStateWithLifecycle()
    val emergencyContact by viewModel.emergencyContact.collectAsStateWithLifecycle()

    LaunchedEffect(sosTriggered) {
        if (sosTriggered) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:$emergencyContact".toUri()
            }
            context.startActivity(intent)
            viewModel.onSosHandled()
        }
    }

    if (isStillTooLong) {
        StillnessCheckDialog(
            onSafe = { viewModel.dismissStillnessCheck() },
            onSOS = { viewModel.triggerSOS() }
        )
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
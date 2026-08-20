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
import androidx.compose.ui.tooling.preview.Preview
import com.example.patheaseapp.ui.home.StillnessCheckDialog
import com.example.patheaseapp.ui.theme.PathEaseAppTheme

// UI screen for the hazard map — observes ViewModel state and renders it, no logic here.
@Composable
fun HazardMapScreen(viewModel: HazardViewModel) {
    val hazards by viewModel.hazards.collectAsStateWithLifecycle()
    val nearbyWarning by viewModel.nearbyWarning.collectAsStateWithLifecycle()
    val isStillTooLong by viewModel.isStillTooLong.collectAsStateWithLifecycle()
    val sosTriggered by viewModel.sosTriggered.collectAsStateWithLifecycle()
    val emergencyContact by viewModel.emergencyContact.collectAsStateWithLifecycle()

    HazardMapScreenContent(
        hazards = hazards,
        nearbyWarning = nearbyWarning,
        isStillTooLong = isStillTooLong,
        sosTriggered = sosTriggered,
        emergencyContact = emergencyContact,
        onSafe = { viewModel.dismissStillnessCheck() },
        onSOS = { viewModel.triggerSOS() },
        onSosHandled = { viewModel.onSosHandled() },
    )
}

@Composable
fun HazardMapScreenContent(
    @Suppress("unused") hazards: List<Hazard>,
    nearbyWarning: Hazard?,
    isStillTooLong: Boolean,
    sosTriggered: Boolean,
    emergencyContact: String,
    onSafe: () -> Unit,
    onSOS: () -> Unit,
    onSosHandled: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(sosTriggered) {
        if (sosTriggered) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:$emergencyContact".toUri()
            }
            context.startActivity(intent)
            onSosHandled()
        }
    }

    if (isStillTooLong) {
        StillnessCheckDialog(
            onSafe = onSafe,
            onSOS = onSOS
        )
    }

    Box(Modifier.fillMaxSize()) {
        nearbyWarning?.let { WarningBanner(it) }
    }
}

@Preview(showBackground = true)
@Composable
fun HazardMapScreenPreview() {
    PathEaseAppTheme {
        HazardMapScreenContent(
            hazards = emptyList(),
            nearbyWarning = Hazard("1", "Pothole", 0.0, 0.0, 15f),
            isStillTooLong = false,
            sosTriggered = false,
            emergencyContact = "999",
            onSafe = {},
            onSOS = {},
            onSosHandled = {}
        )
    }
}
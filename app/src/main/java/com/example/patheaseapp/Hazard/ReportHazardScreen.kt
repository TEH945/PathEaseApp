package com.example.patheaseapp.Hazard

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Screen for submitting a new hazard report: pick a type, attach a photo, submit.
@Composable
fun ReportHazardScreen(
    onSubmit: (type: String, lat: Double, lng: Double) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedType by remember { mutableStateOf("Bumpy Road") }
    var showCamera by remember { mutableStateOf(false) }
    var capturedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    val hazardTypes = listOf("Bumpy Road", "Barrier", "Obstacle", "Pothole")

    if (showCamera) {
        CameraScreen(
            onPhotoCaptured = { bitmap ->
                capturedPhoto = bitmap
                showCamera = false
            },
            onCancel = { showCamera = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Report a Hazard", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Hazard type")
        hazardTypes.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                Text(type)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showCamera = true }) {
            Text(if (capturedPhoto == null) "Take Photo" else "Retake Photo")
        }
        if (capturedPhoto != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Photo attached ✓")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    // TODO: replace with real GPS location once live location tracking is wired up
                    onSubmit(selectedType, 1.4927, 103.7414)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Submit")
            }
        }
    }
}
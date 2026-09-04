package com.example.patheaseapp.Hazard

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHazardScreen(
    currentLocation: LatLng?,
    onSubmit: (type: String, lat: Double, lng: Double, photo: Bitmap?) -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Hazard") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            currentLocation?.let {
                                onSubmit(selectedType, it.latitude, it.longitude, capturedPhoto)
                            }
                        },
                        enabled = currentLocation != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Submit Report")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Hazard type", style = MaterialTheme.typography.titleMedium)
            hazardTypes.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedType = type }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                    Text(type)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (capturedPhoto != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Image(
                        bitmap = capturedPhoto!!.asImageBitmap(),
                        contentDescription = "Captured photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showCamera = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retake Photo")
                }
            } else {
                Button(
                    onClick = { showCamera = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take Photo")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            if (currentLocation == null) {
                Text(
                    "Waiting for your location...",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

package com.example.patheaseapp.Hazard

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

// Camera screen for capturing a photo of the hazard being reported.
@Composable
fun CameraScreen(
    onPhotoCaptured: (Bitmap?) -> Unit,
    onCancel: () -> Unit,
) {
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> capturedBitmap = bitmap }

    LaunchedEffect(Unit) { cameraLauncher.launch() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap!!.asImageBitmap(),
                contentDescription = "Captured hazard photo",
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { onPhotoCaptured(capturedBitmap) }) { Text("Use Photo") }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(onClick = { cameraLauncher.launch() }) { Text("Retake") }
            }
        } else {
            Text("Opening camera...")
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
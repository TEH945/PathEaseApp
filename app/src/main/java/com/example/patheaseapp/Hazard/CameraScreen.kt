package com.example.patheaseapp.Hazard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun CameraScreen(
    onPhotoCaptured: (Bitmap?) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> capturedBitmap = bitmap }

    fun launchCameraSafely() {
        try {
            cameraLauncher.launch(null)
        } catch (e: Exception) {
            Toast.makeText(context, "No camera app available on this device.", Toast.LENGTH_SHORT).show()
            onCancel()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraSafely()
        } else {
            Toast.makeText(context, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show()
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchCameraSafely()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
                OutlinedButton(onClick = { launchCameraSafely() }) { Text("Retake") }
            }
        } else {
            Text("Opening camera...")
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
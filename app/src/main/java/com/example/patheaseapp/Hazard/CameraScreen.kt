package com.example.patheaseapp.Hazard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onPhotoCaptured: (Bitmap?) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show()
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun takePhoto() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    capturedBitmap = image.toBitmap()
                    image.close()
                }
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(context, "Failed to capture photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            capturedBitmap != null -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Photo Preview", color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { capturedBitmap = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Retake", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                        )
                    },
                    bottomBar = {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { capturedBitmap = null },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White)
                                ) { Text("Retake") }
                                
                                Button(
                                    onClick = { onPhotoCaptured(capturedBitmap) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) { Text("Use Photo") }
                            }
                        }
                    },
                    containerColor = Color.Black
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured hazard photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            hasPermission -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Unable to start camera.", Toast.LENGTH_SHORT).show()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )

                // Close button at top-left
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }

                // Shutter button at bottom-center, raised to clear navigation bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 80.dp) 
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(onClick = ::takePhoto),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Requesting camera permission...")
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

// Converts the JPEG frame CameraX delivers into a Bitmap for preview/upload,
// and applies the correct rotation so the photo isn't sideways.
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
package com.example.patheaseapp.Hazard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

// Shown when a user taps a hazard marker on the map — shows what it is,
// a photo if one was submitted, and lets other users confirm it's gone.
@Composable
fun HazardDetailDialog(
    hazard: Hazard,
    onConfirmRemoved: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(hazard.type, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(12.dp))

                if (hazard.photoUrl != null) {
                    AsyncImage(
                        model = hazard.photoUrl,
                        contentDescription = "Photo of ${hazard.type}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    Text("No photo submitted", color = MaterialTheme.colorScheme.outline)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Reported removed ${hazard.confirmedRemovedCount} time(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onConfirmRemoved, modifier = Modifier.fillMaxWidth()) {
                    Text("This hazard has been removed")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}
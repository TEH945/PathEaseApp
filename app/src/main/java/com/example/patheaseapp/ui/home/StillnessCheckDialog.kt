package com.example.patheaseapp.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.patheaseapp.ui.theme.PathEaseAppTheme

// Dialog shown when the user has been stationary too long, asking if they're safe.
@Composable
fun StillnessCheckDialog(
    onSafe: () -> Unit,
    onSOS: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* force a choice, don't dismiss on outside tap */ },
        title = { Text(text = "You've been still for a while") },
        text = { Text(text = "We noticed you haven't moved in 5 minutes. Are you safe?") },
        confirmButton = {
            Button(onClick = onSafe) {
                Text(text = "I'm Safe")
            }
        },
        dismissButton = {
            Button(
                onClick = onSOS,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            ) {
                @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                Text(text = "Need Help (SOS)")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun StillnessCheckDialogPreview() {
    PathEaseAppTheme {
        StillnessCheckDialog(
            onSafe = {},
        ) { }
    }
}

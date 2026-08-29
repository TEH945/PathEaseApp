package com.example.patheaseapp.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    supabaseClient: SupabaseClient,
    onBack: () -> Unit,
    onSuccess: () -> Unit = {},
    isRecoveryMode: Boolean = false, // Set to true if app opened via recovery link
) {
    var email by remember { mutableStateOf(value = "") }
    var newPassword by remember { mutableStateOf(value = "") }
    var confirmPassword by remember { mutableStateOf(value = "") }
    var passwordVisible by remember { mutableStateOf(value = false) }
    var isLoading by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(value = null) }
    var successMessage by remember { mutableStateOf<String?>(value = null) }
    
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()
    val isSessionReady = sessionStatus is SessionStatus.Authenticated
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (isRecoveryMode) "Set New Password" else "Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(all = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isRecoveryMode) {
                // Step 1: Request Reset Link
                Text(
                    text = "Enter your email to receive a password reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null; successMessage = null },
                    label = { Text(text = "Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(height = 24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (email.isBlank()) {
                                errorMessage = "Please enter your email"
                                return@launch
                            }
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            try {
                                // Specify the redirect URL so it matches your Android Manifest
                                supabaseClient.auth.resetPasswordForEmail(
                                    email = email.trim(),
                                    redirectUrl = "patheaseapp://reset-password",
                                )
                                successMessage = "Reset link sent! Please check your email inbox."
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "An unexpected error occurred"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(size = 24.dp))
                    } else {
                        Text(text = "Send Reset Link")
                    }
                }
            } else {
                // Step 2: Set New Password (User returned from email link)
                Text(
                    text = "Please enter your new password below.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text(text = "New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(height = 16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text(text = "Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                Spacer(modifier = Modifier.height(height = 24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (!isSessionReady) {
                                errorMessage = "Session not ready yet. Please wait a moment or try opening the link again."
                                return@launch
                            }
                            if (newPassword.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                return@launch
                            }
                            if (newPassword != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@launch
                            }
                            isLoading = true
                            errorMessage = null
                            try {
                                supabaseClient.auth.updateUser {
                                    password = newPassword
                                }
                                successMessage = "Password updated! You can now log in with your new password."
                                delay(2000.milliseconds)
                                onSuccess()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "An unexpected error occurred"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(size = 24.dp))
                    } else if (!isSessionReady) {
                        Text(text = "Waiting for session...")
                    } else {
                        Text(text = "Update Password")
                    }
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(height = 16.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            successMessage?.let {
                Spacer(modifier = Modifier.height(height = 16.dp))
                Text(text = it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

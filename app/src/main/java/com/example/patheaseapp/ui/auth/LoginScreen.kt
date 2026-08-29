package com.example.patheaseapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Suppress("unused")
@Composable
fun LoginScreen(
    supabaseClient: SupabaseClient,
    onForgotPassword: () -> Unit,
) {
    var email by remember { mutableStateOf(value = "") }
    var name by remember { mutableStateOf(value = "") }
    var password by remember { mutableStateOf(value = "") }
    var passwordVisible by remember { mutableStateOf(value = false) }
    var isLoading by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(value = null) }
    var successMessage by remember { mutableStateOf<String?>(value = null) }
    var isSignUp by remember { mutableStateOf(value = false) }

    // Clear messages when switching between Login and Sign Up
    LaunchedEffect(isSignUp) {
        errorMessage = null
        // Clear success message ONLY when moving TO the Sign Up screen
        if (isSignUp) {
            successMessage = null
        }
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(height = 32.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                errorMessage = null
                successMessage = null
            },
            label = { Text(text = "Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
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

        if (!isSignUp) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = onForgotPassword,
                    enabled = !isLoading,
                ) {
                    Text(text = "Forgot Password?", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(height = 16.dp))
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        successMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        Spacer(modifier = Modifier.height(height = 8.dp))

        Button(
            onClick = {
                if (isLoading) return@Button // Extra safety check
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val trimmedEmail = email.trim()
                        if (isSignUp) {
                            supabaseClient.auth.signUpWith(Email) {
                                this.email = trimmedEmail
                                this.password = password
                                // Store name in both "name" and "full_name" for compatibility
                                data = buildJsonObject {
                                    put(key = "name", value = name.trim())
                                    put(key = "full_name", value = name.trim())
                                }
                            }
                            // After successful sign up, show message and jump to Sign In page
                            successMessage = "Verification email sent! Please check your inbox."
                            isSignUp = false
                        } else {
                            supabaseClient.auth.signInWith(Email) {
                                this.email = trimmedEmail
                                this.password = password
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "An unexpected error occurred"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && (!isSignUp || name.isNotBlank()),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = 24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(text = if (isSignUp) "Create account" else "Sign In")
            }
        }

        TextButton(
            onClick = { isSignUp = !isSignUp },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
        }
    }
}

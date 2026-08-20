package com.example.patheaseapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@Suppress("unused")
@Composable
fun LoginScreen(
    supabaseClient: SupabaseClient,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSignUp by remember { mutableStateOf(false) }

    // Clear messages when switching between Login and Sign Up
    LaunchedEffect(isSignUp) {
        errorMessage = null
        successMessage = null
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                errorMessage = null
                successMessage = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        if (!isSignUp) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (email.isBlank()) {
                                errorMessage = "Please enter your email first"
                                return@launch
                            }
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            try {
                                supabaseClient.auth.resetPasswordForEmail(email.trim())
                                successMessage = "Reset link sent to your email"
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to send reset email"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        successMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val trimmedEmail = email.trim()
                        if (isSignUp) {
                            supabaseClient.auth.signUpWith(Email) {
                                this.email = trimmedEmail
                                this.password = password
                            }
                        } else {
                            supabaseClient.auth.signInWith(Email) {
                                this.email = trimmedEmail
                                this.password = password
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Authentication failed"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (isSignUp) "Sign Up" else "Sign In")
            }
        }

        TextButton(
            onClick = { isSignUp = !isSignUp },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
        }
    }
}

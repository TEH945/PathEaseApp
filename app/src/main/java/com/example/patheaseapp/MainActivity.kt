package com.example.patheaseapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.patheaseapp.data.local.AccessibilityRepository
import com.example.patheaseapp.ui.theme.PathEaseAppTheme
import com.google.android.libraries.places.api.Places
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseFragmentAndImportSession
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isRecoveryFlow by mutableStateOf(false)

    private val supabaseClient by lazy {
        @OptIn(SupabaseInternal::class)
        createSupabaseClient(
            supabaseUrl = "https://mmdkfjptbjkabbspuzfq.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1tZGtmanB0YmprYWJic3B1emZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcyMjE5NjcsImV4cCI6MjEwMjc5Nzk2N30.sEOCyQgX6qnvgT392f-uatqj3Wga-NfhOdbblpGhkz8",
        ) {
            install(Postgrest)
            install(Auth)
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000L
                    connectTimeoutMillis = 60000L
                    socketTimeoutMillis = 60000L
                }
            }
        }
    }

    @OptIn(SupabaseInternal::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAnaGQ6_M6kAVKKrRPovxoccon0jyb4aik")
        }

        enableEdgeToEdge()
        setContent {
            PathEaseAppTheme {
                val context = LocalContext.current
                val accessibilityRepo = remember { AccessibilityRepository(context.applicationContext) }
                val accessibilityState by accessibilityRepo.accessibilityState.collectAsStateWithLifecycle()

                LaunchedEffect(accessibilityState.keepScreenOn) {
                    val window = (context as? Activity)?.window
                    if (accessibilityState.keepScreenOn) {
                        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                PathEaseApp(
                    supabaseClient = supabaseClient,
                    accessibilityRepo = accessibilityRepo,
                    initialForgotPasswordVisible = isRecoveryFlow,
                    onForgotPasswordVisibleChanged = { isRecoveryFlow = it }
                )
            }
        }
    }

    @OptIn(SupabaseInternal::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) 
        handleDeepLink(intent)
    }

    @OptIn(SupabaseInternal::class)
    private fun handleDeepLink(intent: Intent) {
        intent.data?.let { uri ->
            val uriString = uri.toString()
            Log.d("SupabaseAuth", "Handling deep link: $uriString")
            
            if (uriString.contains("access_token=")) {
                isRecoveryFlow = true
                lifecycleScope.launch {
                    try {
                        // More robust fragment/query parsing
                        val fragment = uriString.substringAfter("#").ifEmpty { uriString.substringAfter("?") }
                        val params = fragment.split("&").filter { it.contains("=") }.associate {
                            val parts = it.split("=")
                            parts[0] to parts[1]
                        }
                        
                        val accessToken = params["access_token"]
                        val refreshToken = params["refresh_token"]
                        
                        if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                            Log.d("SupabaseAuth", "Importing session from params")
                            val session = UserSession(
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                expiresIn = params["expires_in"]?.toLongOrNull() ?: 3600L,
                                tokenType = params["token_type"] ?: "bearer",
                                user = null
                            )
                            supabaseClient.auth.importSession(session)
                        } else {
                            Log.d("SupabaseAuth", "Parsing fragment and importing session")
                            supabaseClient.auth.parseFragmentAndImportSession(uriString.replace("?", "#"))
                        }
                    } catch (e: Exception) {
                        Log.e("SupabaseAuth", "Deep link import failed", e)
                    }
                }
            }
        }
    }
}

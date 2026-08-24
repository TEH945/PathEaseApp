package com.example.patheaseapp

import android.os.Bundle
import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.patheaseapp.data.local.AccessibilityRepository
import com.example.patheaseapp.ui.theme.PathEaseAppTheme
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseFragmentAndImportSession
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // Define Supabase client at activity level to handle deep links
    @OptIn(SupabaseInternal::class)
    private val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mmdkfjptbjkabbspuzfq.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1tZGtmanB0YmprYWJic3B1emZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcyMjE5NjcsImV4cCI6MjEwMjc5Nzk2N30.sEOCyQgX6qnvgT392f-uatqj3Wga-NfhOdbblpGhkz8"
        ) {
            install(Postgrest)
            install(Auth)
            httpConfig {
                install(io.ktor.client.plugins.HttpTimeout) {
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
        
        // Handle deep link when app is created
        intent?.data?.let { uri ->
            lifecycleScope.launch {
                try {
                    supabaseClient.auth.parseFragmentAndImportSession(uri.toString())
                    android.util.Log.d("SupabaseAuth", "Session imported successfully from fragment")
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseAuth", "Failed to import session: ${e.message}")
                }
            }
        }
        
        enableEdgeToEdge()
        setContent {
            PathEaseAppTheme {
                val accessibilityRepo = remember { AccessibilityRepository(applicationContext) }
                val accessibilityState by accessibilityRepo.accessibilityState.collectAsState()

                // Handle Keep Screen On setting
                LaunchedEffect(accessibilityState.keepScreenOn) {
                    if (accessibilityState.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                PathEaseApp(supabaseClient, accessibilityRepo)
            }
        }
    }

    @OptIn(SupabaseInternal::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Important: update the intent
        intent?.data?.let { uri ->
            lifecycleScope.launch {
                try {
                    supabaseClient.auth.parseFragmentAndImportSession(uri.toString())
                    android.util.Log.d("SupabaseAuth", "Session imported successfully onNewIntent")
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseAuth", "Failed to import session onNewIntent: ${e.message}")
                }
            }
        }
    }
}
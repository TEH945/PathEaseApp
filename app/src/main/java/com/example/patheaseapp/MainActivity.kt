package com.example.patheaseapp

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.patheaseapp.ui.theme.PathEaseAppTheme
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseFragmentAndImportSession

class MainActivity : ComponentActivity() {
    
    // Define Supabase client at activity level to handle deep links
    private val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://mmdkfjptbjkabbspuzfq.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1tZGtmanB0YmprYWJic3B1emZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcyMjE5NjcsImV4cCI6MjEwMjc5Nzk2N30.sEOCyQgX6qnvgT392f-uatqj3Wga-NfhOdbblpGhkz8"
        ) {
            install(Postgrest)
            install(Auth)
        }
    }

    @OptIn(SupabaseInternal::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle deep link when app is created
        intent?.data?.let { uri ->
            supabaseClient.auth.parseFragmentAndImportSession(uri.toString())
        }
        
        enableEdgeToEdge()
        setContent {
            PathEaseAppTheme {
                PathEaseApp(supabaseClient)
            }
        }
    }

    @OptIn(SupabaseInternal::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running
        intent?.data?.let { uri ->
            supabaseClient.auth.parseFragmentAndImportSession(uri.toString())
        }
    }
}
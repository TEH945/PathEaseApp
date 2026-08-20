package com.example.patheaseapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.patheaseapp.data.local.AccessibilityRepository
import com.example.patheaseapp.ui.home.HomeScreen
import com.example.patheaseapp.ui.home.HomeViewModel
import com.example.patheaseapp.ui.profile.AccessibilitySettingsScreen
import com.example.patheaseapp.ui.profile.HistoryScreen
import com.example.patheaseapp.ui.profile.ProfileScreen
import com.example.patheaseapp.ui.profile.ProfileViewModel
import com.example.patheaseapp.ui.profile.SettingsScreen
import com.example.patheaseapp.ui.profile.StarredLocationsScreen
import com.example.patheaseapp.ui.theme.PathEaseAppTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import androidx.compose.ui.platform.LocalContext
import com.example.patheaseapp.ui.auth.LoginScreen

sealed class Screen(@Suppress("unused") val route: String, val title: String, val icon: ImageVector) {
    object History : Screen("history", "History", Icons.Default.History)
    object Starred : Screen("starred", "Starred", Icons.Default.Star)
    object Map : Screen("map", "Map", Icons.Default.Map)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Accessibility : Screen("accessibility", "Accessibility", Icons.Default.Settings)
}

@Composable
fun PathEaseApp() {
    val context = LocalContext.current
    
    // Initialize dummy Supabase client (Replace with real credentials later)
    val supabaseClient = remember {
        createSupabaseClient(
            supabaseUrl = "https://mmdkfjptbjkabbspuzfq.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1tZGtmanB0YmprYWJic3B1emZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcyMjE5NjcsImV4cCI6MjEwMjc5Nzk2N30.sEOCyQgX6qnvgT392f-uatqj3Wga-NfhOdbblpGhkz8"
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
    
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()
    val session = (sessionStatus as? SessionStatus.Authenticated)?.session
    
    if (session == null) {
        LoginScreen(supabaseClient = supabaseClient)
    } else {
        val userId = session.user?.id ?: ""
        
        val accessibilityRepo = remember { AccessibilityRepository(context) }
        
        var currentTab by remember { mutableStateOf<Screen>(Screen.Map) }
        val homeViewModel: HomeViewModel = viewModel()
        
        // We'll need a way to create ProfileViewModel with its dependencies
        val profileViewModel: ProfileViewModel = viewModel(
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ProfileViewModel(supabaseClient, accessibilityRepo) as T
                }
            }
        )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Screen.History,
                        Screen.Starred,
                        Screen.Map,
                        Screen.Settings,
                        Screen.Profile,
                    )
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentTab == screen,
                            onClick = { currentTab = screen },
                            modifier = Modifier.semantics {
                                contentDescription = "Navigate to ${screen.title} screen"
                            },
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (currentTab) {
                Screen.Map -> HomeScreen(
                    viewModel = homeViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.History -> HistoryScreen(
                    viewModel = profileViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.Starred -> StarredLocationsScreen(
                    viewModel = profileViewModel,
                    currentUserId = userId,
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.Settings -> SettingsScreen(
                    onNavigateToAccessibility = { currentTab = Screen.Accessibility },
                    onNavigateToStarred = { currentTab = Screen.Starred },
                    onNavigateToHistory = { currentTab = Screen.History },
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.Accessibility -> AccessibilitySettingsScreen(
                    viewModel = profileViewModel,
                    onBack = { currentTab = Screen.Settings },
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.Profile -> ProfileScreen(
                    viewModel = profileViewModel,
                    userId = userId,
                    onNavigateToSettings = { currentTab = Screen.Settings },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PathEaseAppPreview() {
    PathEaseAppTheme {
        PathEaseApp()
    }
}
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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import androidx.compose.ui.platform.LocalContext

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
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
            supabaseUrl = "https://your-project.supabase.co",
            supabaseKey = "your-anon-key"
        ) {
            install(Postgrest)
        }
    }
    
    val accessibilityRepo = remember { AccessibilityRepository(context) }
    
    var currentTab by remember { mutableStateOf<Screen>(Screen.Map) }
    val homeViewModel: HomeViewModel = viewModel()
    
    // We'll need a way to create ProfileViewModel with its dependencies
    val profileViewModel: ProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
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
                modifier = Modifier.padding(innerPadding)
            )
            Screen.History -> HistoryScreen(
                viewModel = profileViewModel,
                modifier = Modifier.padding(innerPadding)
            )
            Screen.Starred -> StarredLocationsScreen(
                viewModel = profileViewModel,
                currentUserId = "dummy-user-id", // Should come from Auth
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
                onNavigateToSettings = { currentTab = Screen.Settings },
                modifier = Modifier.padding(innerPadding)
            )
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
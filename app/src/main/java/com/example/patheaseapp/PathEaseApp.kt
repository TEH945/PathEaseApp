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
import com.example.patheaseapp.ui.auth.ForgotPasswordScreen

sealed class Screen(@Suppress("unused") val route: String, val title: String, val icon: ImageVector) {
    object History : Screen("history", "History", Icons.Default.History)
    object Starred : Screen("starred", "Starred", Icons.Default.Star)
    object Map : Screen("map", "Map", Icons.Default.Map)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Accessibility : Screen("accessibility", "Accessibility", Icons.Default.Settings)
}

@Composable
fun PathEaseApp(
    supabaseClient: io.github.jan.supabase.SupabaseClient,
    accessibilityRepo: AccessibilityRepository
) {
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()
    val session = (sessionStatus as? SessionStatus.Authenticated)?.session

    var isForgotPasswordVisible by remember { mutableStateOf(false) }
    var isRecoveryMode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val intent = (context as? android.app.Activity)?.intent

    // Detect if we entered via a recovery link
    LaunchedEffect(sessionStatus, intent) {
        val uri = intent?.data
        if (uri != null && uri.toString().contains("type=recovery")) {
            isRecoveryMode = true
        } else if (sessionStatus is SessionStatus.Authenticated && isForgotPasswordVisible) {
            isRecoveryMode = true
        }
    }

    if (session == null || isRecoveryMode) {
        if (isForgotPasswordVisible || isRecoveryMode) {
            ForgotPasswordScreen(
                supabaseClient = supabaseClient,
                onBack = { 
                    isForgotPasswordVisible = false
                    isRecoveryMode = false
                },
                onSuccess = {
                    isForgotPasswordVisible = false
                    isRecoveryMode = false
                },
                isRecoveryMode = isRecoveryMode
            )
        } else {
            LoginScreen(
                supabaseClient = supabaseClient,
                onForgotPassword = { isForgotPasswordVisible = true }
            )
        }
    } else {
        // If we just logged in via a recovery link, we might want to force the Reset screen
        // For simplicity, we can let the user manually go to profile or handle it via a flag
        // Here we handle the post-login "Recovery" state if needed
        val userId = session.user?.id ?: ""
        
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
                    homeViewModel = homeViewModel,
                    profileViewModel = profileViewModel,
                    userId = userId,
                    modifier = Modifier.padding(innerPadding),
                )
                Screen.History -> HistoryScreen(
                    viewModel = profileViewModel,
                    userId = userId,
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.Starred -> StarredLocationsScreen(
                    viewModel = profileViewModel,
                    currentUserId = userId,
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.Settings -> SettingsScreen(
                    viewModel = profileViewModel,
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
    // For preview, we still need a client, but we can't easily create one here.
    // In a real app, you'd use a composition local or a mock.
}
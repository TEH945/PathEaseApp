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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.patheaseapp.data.local.AccessibilityRepository
import com.example.patheaseapp.ui.auth.ForgotPasswordScreen
import com.example.patheaseapp.ui.auth.LoginScreen
import com.example.patheaseapp.ui.home.HomeScreen
import com.example.patheaseapp.ui.home.HomeViewModel
import com.example.patheaseapp.ui.profile.AccessibilitySettingsScreen
import com.example.patheaseapp.ui.profile.HistoryScreen
import com.example.patheaseapp.ui.profile.ProfileScreen
import com.example.patheaseapp.ui.profile.ProfileViewModel
import com.example.patheaseapp.ui.profile.SettingsScreen
import com.example.patheaseapp.ui.profile.StarredLocationsScreen
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

sealed class AppScreen(@Suppress("unused") val route: String, val title: String, val icon: ImageVector) {
    object History : AppScreen("history", "History", Icons.Default.History)
    object Starred : AppScreen("starred", "Starred", Icons.Default.Star)
    object Map : AppScreen("map", "Map", Icons.Default.Map)
    object Settings : AppScreen("settings", "Settings", Icons.Default.Settings)
    object Profile : AppScreen("profile", "Profile", Icons.Default.Person)
    object Accessibility : AppScreen("accessibility", "Accessibility", Icons.Default.Settings)
}

@Composable
fun PathEaseApp(
    supabaseClient: SupabaseClient,
    accessibilityRepo: AccessibilityRepository,
    initialForgotPasswordVisible: Boolean = false,
    onForgotPasswordVisibleChanged: (Boolean) -> Unit = {},
) {
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()
    val session = (sessionStatus as? SessionStatus.Authenticated)?.session

    var isForgotPasswordVisible by rememberSaveable { mutableStateOf(initialForgotPasswordVisible) }
    
    // Sync state with parent if needed
    LaunchedEffect(initialForgotPasswordVisible) {
        if (initialForgotPasswordVisible) {
            isForgotPasswordVisible = true
        }
    }

    LaunchedEffect(isForgotPasswordVisible) {
        onForgotPasswordVisibleChanged(isForgotPasswordVisible)
    }
    
    // 如果 session 已存在，说明用户已登录（包括通过重置密码链接进入）
    val isRecoveryMode = session != null && isForgotPasswordVisible

    if (session == null || (isRecoveryMode)) {
        if (isForgotPasswordVisible) {
            ForgotPasswordScreen(
                supabaseClient = supabaseClient,
                onBack = { isForgotPasswordVisible = false },
                onSuccess = { isForgotPasswordVisible = false },
                isRecoveryMode = session != null // 只要有 session 且在忘记密码页，就视为恢复模式
            )
        } else {
            LoginScreen(
                supabaseClient = supabaseClient,
                onForgotPassword = { isForgotPasswordVisible = true }
            )
        }
    } else {
        val userId = session.user?.id ?: ""
        var currentTab by remember { mutableStateOf<AppScreen>(AppScreen.Map) }
        var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
        var selectedName by remember { mutableStateOf<String?>(null) }
        var selectedAddress by remember { mutableStateOf<String?>(null) }

        val homeViewModel: HomeViewModel = viewModel<HomeViewModel>()
        
        val profileViewModel: ProfileViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ProfileViewModel(supabaseClient, accessibilityRepo) as T
                }
            },
        )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        AppScreen.History,
                        AppScreen.Starred,
                        AppScreen.Map,
                        AppScreen.Settings,
                        AppScreen.Profile,
                    )
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(text = screen.title) },
                            selected = currentTab == screen,
                            onClick = { currentTab = screen },
                            modifier = Modifier.semantics {
                                contentDescription = "Navigate to ${screen.title} screen"
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            when (currentTab) {
                AppScreen.Map -> HomeScreen(
                    homeViewModel = homeViewModel,
                    profileViewModel = profileViewModel,
                    userId = userId,
                    initialLocation = selectedLocation,
                    initialName = selectedName,
                    initialAddress = selectedAddress,
                    onLocationReset = {
                        selectedLocation = null
                        selectedName = null
                        selectedAddress = null
                    },
                    modifier = Modifier.padding(innerPadding),
                )
                AppScreen.History -> HistoryScreen(
                    viewModel = profileViewModel,
                    userId = userId,
                    onHistorySelected = { historyItem ->
                        if (historyItem.destinationLatitude != null && historyItem.destinationLongitude != null) {
                            selectedLocation = LatLng(
                                historyItem.destinationLatitude,
                                historyItem.destinationLongitude
                            )
                            selectedName = historyItem.destination
                            selectedAddress = "From History"
                            currentTab = AppScreen.Map
                        }
                    },
                    modifier = Modifier.padding(innerPadding),
                )
                AppScreen.Starred -> StarredLocationsScreen(
                    viewModel = profileViewModel,
                    currentUserId = userId,
                    onLocationSelected = { location ->
                        selectedLocation = LatLng(location.latitude, location.longitude)
                        selectedName = location.name
                        selectedAddress = location.address
                        currentTab = AppScreen.Map
                    },
                    modifier = Modifier.padding(innerPadding),
                )
                AppScreen.Settings -> SettingsScreen(
                    viewModel = profileViewModel,
                    onNavigateToAccessibility = { currentTab = AppScreen.Accessibility },
                    modifier = Modifier.padding(innerPadding),
                )
                AppScreen.Accessibility -> AccessibilitySettingsScreen(
                    viewModel = profileViewModel,
                    onBack = { currentTab = AppScreen.Settings },
                    modifier = Modifier.padding(innerPadding),
                )
                AppScreen.Profile -> ProfileScreen(
                    viewModel = profileViewModel,
                    userId = userId,
                    onNavigateToSettings = { currentTab = AppScreen.Settings },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

package com.example.patheaseapp.ui

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
import com.example.patheaseapp.ui.home.HomeScreen
import com.example.patheaseapp.ui.home.HomeViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object History : Screen("history", "History", Icons.Default.History)
    object Starred : Screen("starred", "Starred", Icons.Default.Star)
    object Map : Screen("map", "Map", Icons.Default.Map)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun PathEaseApp() {
    var currentTab by remember { mutableStateOf<Screen>(Screen.Map) }
    val homeViewModel: HomeViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Screen.History,
                    Screen.Starred,
                    Screen.Map,
                    Screen.Settings,
                    Screen.Profile
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentTab == screen,
                        onClick = { currentTab = screen },
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate to ${screen.title} screen"
                        }
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
            else -> {
                // Reserved for other group members' screens
            }
        }
    }
}
package com.example.patheaseapp.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.patheaseapp.data.local.AccessibilityPreferences
import com.example.patheaseapp.data.remote.RouteHistoryItem
import com.example.patheaseapp.data.remote.SupabaseProfile
import com.example.patheaseapp.data.remote.SupabaseStartedLocation
import com.example.patheaseapp.ui.theme.PathEaseAppTheme

// --- 1. PROFILE SCREEN ---
@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: String,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isProfileLoading.collectAsState()
    val error by viewModel.profileError.collectAsState()

    LaunchedEffect(userId) {
        viewModel.fetchProfile(userId)
    }
    
    ProfileScreenContent(
        profile = profile,
        isLoading = isLoading,
        error = error,
        onUpdateProfile = { name, email, contact -> viewModel.updateProfile(userId, name, email, contact) },
        onLogout = { viewModel.logout() },
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    profile: SupabaseProfile?,
    isLoading: Boolean,
    error: String?,
    onUpdateProfile: (String, String, String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }
    
    // Use remember(profile) to sync inputs whenever the profile data updates from server
    var nameInput by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var emailInput by remember(profile) { mutableStateOf(profile?.email ?: "") }
    var emergencyContactInput by remember(profile) { mutableStateOf(profile?.emergencyContact ?: "999") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics { contentDescription = "Open app settings button" },
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isEditing) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { /* nameInput = it */ }, // User cannot edit
                    label = { Text("Name") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Name field (read-only)" },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { /* emailInput = it */ }, // User cannot edit
                    label = { Text("Email") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Email field (read-only)" },
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyContactInput,
                    onValueChange = { emergencyContactInput = it },
                    label = { Text("emergency Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Edit phone number field" },
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, emailInput, emergencyContactInput)
                        isEditing = false
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Save profile changes button" },
                ) {
                    Text("Save Profile")
                }
            } else {
                Text(text = profile?.name.takeIf { !it.isNullOrBlank() } ?: "Guest User", style = MaterialTheme.typography.headlineMedium)
                Text(text = profile?.email.takeIf { !it.isNullOrBlank() } ?: "No email set", style = MaterialTheme.typography.bodyMedium)
                Text(text = profile?.emergencyContact.takeIf { !it.isNullOrBlank() } ?: "999", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        nameInput = profile?.name ?: ""
                        emailInput = profile?.email ?: ""
                        emergencyContactInput = profile?.emergencyContact.takeIf { !it.isNullOrBlank() } ?: "999"
                        isEditing = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Edit profile details button" },
                ) {
                    Text("Edit Profile")
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Log out account button" },
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }
        }
    }
}

// --- 2. SETTINGS SCREEN ---
@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToStarred: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.accessibilitySettings.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ListItem(
                headlineContent = { Text("Accessibility Settings") },
                supportingContent = { Text("Visual mode, voice guidance, wheelchair, stroller options") },
                leadingContent = { Icon(Icons.Default.AccessibilityNew, contentDescription = null) },
                modifier = Modifier
                    .clickable { onNavigateToAccessibility() }
                    .semantics { contentDescription = "Navigate to accessibility settings" },
            )
            HorizontalDivider()
            
            // "Don't lock the screen" toggle moved here
            ListItem(
                headlineContent = { Text("Don't lock the screen") },
                supportingContent = { Text("Keep the display active while the app is open") },
                trailingContent = {
                    Switch(
                        checked = settings.keepScreenOn,
                        onCheckedChange = { viewModel.toggleKeepScreenOn(it) }
                    )
                },
                modifier = Modifier.semantics { contentDescription = "Toggle screen lock prevention" }
            )
        }
    }
}

// --- 3. ACCESSIBILITY SETTINGS SCREEN ---
@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.accessibilitySettings.collectAsState()

    AccessibilitySettingsScreenContent(
        settings = settings,
        onToggleVisualAssistance = { viewModel.toggleVisualAssistance(it) },
        onToggleVoiceGuidance = { viewModel.toggleVoiceGuidance(it) },
        onToggleWheelchairAccess = { viewModel.toggleWheelchairAccess(it) },
        onToggleStrollerMode = { viewModel.toggleStrollerMode(it) },
        onToggleBlindMode = { viewModel.toggleBlindMode(it) },
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreenContent(
    settings: AccessibilityPreferences,
    onToggleVisualAssistance: (Boolean) -> Unit,
    onToggleVoiceGuidance: (Boolean) -> Unit,
    onToggleWheelchairAccess: (Boolean) -> Unit,
    onToggleStrollerMode: (Boolean) -> Unit,
    onToggleBlindMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            AccessibilityToggleRow(
                title = "Visual Assistance Mode",
                description = "Enables high contrast and extra screen reader detail",
                checked = settings.visualAssistanceMode,
                onCheckedChange = onToggleVisualAssistance,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccessibilityToggleRow(
                title = "Voice Guidance",
                description = "Turn-by-turn spoken prompts during navigation",
                checked = settings.voiceGuidanceEnabled,
                onCheckedChange = onToggleVoiceGuidance,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccessibilityToggleRow(
                title = "Blind Mode",
                description = "Optimized interface and specific audio cues for blind users",
                checked = settings.blindModeEnabled,
                onCheckedChange = onToggleBlindMode,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccessibilityToggleRow(
                title = "Wheelchair Accessible Mode",
                description = "Prioritizes routes with elevators and ramps",
                checked = settings.wheelchairAccessEnabled,
                onCheckedChange = onToggleWheelchairAccess,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccessibilityToggleRow(
                title = "Stroller Friendly Mode",
                description = "Avoids stairs and narrow pathways",
                checked = settings.strollerModeEnabled,
                onCheckedChange = onToggleStrollerMode,
            )
        }
    }
}

@Composable
fun AccessibilityToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title switch is currently ${if (checked) "on" else "off"}. $description"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// --- 4. STARRED LOCATIONS SCREEN ---
@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredLocationsScreen(
    viewModel: ProfileViewModel,
    currentUserId: String,
    modifier: Modifier = Modifier,
) {
    val locations by viewModel.starredLocations.collectAsState()
    LaunchedEffect(currentUserId) {
        viewModel.fetchStarredLocations(currentUserId)
    }

    StarredLocationsScreenContent(
        locations = locations,
        onDeleteLocation = { locationId -> viewModel.deleteStarredLocation(currentUserId, locationId) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredLocationsScreenContent(
    locations: List<SupabaseStartedLocation>,
    onDeleteLocation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Starred Locations") }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(locations, key = { it.id ?: it.hashCode().toString() }) { location ->
                ListItem(
                    headlineContent = { Text(location.name) },
                    supportingContent = { Text(location.address) },
                    trailingContent = {
                        IconButton(
                            onClick = { location.id?.let { onDeleteLocation(it) } },
                            modifier = Modifier.semantics { contentDescription = "Remove ${location.name} from starred locations" },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

// --- 5. HISTORY SCREEN ---
@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ProfileViewModel,
    userId: String,
    modifier: Modifier = Modifier,
) {
    val history by viewModel.routeHistory.collectAsState()

    LaunchedEffect(userId) {
        viewModel.fetchRouteHistory(userId)
    }

    HistoryScreenContent(
        history = history,
        onClearHistory = { viewModel.clearHistory() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    history: List<RouteHistoryItem>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Route History") },
                actions = {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.semantics { contentDescription = "Clear all route history button" },
                    ) {
                        Text("Clear All")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(history, key = { it.id ?: it.hashCode().toString() }) { item ->
                ListItem(
                    headlineContent = { Text("${item.origin} ➔ ${item.destination}") },
                    supportingContent = { Text(item.timestamp) },
                    leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                )
                HorizontalDivider()
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    // In a real app, you'd use a mock ViewModel
}

@Preview(showBackground = true)
@Composable
fun AccessibilitySettingsScreenPreview() {
    PathEaseAppTheme {
        AccessibilitySettingsScreenContent(
            settings = AccessibilityPreferences(),
            onToggleVisualAssistance = {},
            onToggleVoiceGuidance = {},
            onToggleWheelchairAccess = {},
            onToggleStrollerMode = {},
            onToggleBlindMode = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StarredLocationsScreenPreview() {
    PathEaseAppTheme {
        StarredLocationsScreenContent(
            locations = listOf(
                SupabaseStartedLocation("1", "user1", "Home", "123 Main St", 0.0, 0.0),
                SupabaseStartedLocation("2", "user1", "Office", "456 Work Ave", 0.0, 0.0)
            ),
            onDeleteLocation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    PathEaseAppTheme {
        HistoryScreenContent(
            history = listOf(
                RouteHistoryItem("1", "user1", "Home", "Office", "2023-10-01 08:00"),
                RouteHistoryItem("2", "user1", "Office", "Home", "2023-10-01 17:00")
            ),
            onClearHistory = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    PathEaseAppTheme {
        ProfileScreenContent(
            profile = SupabaseProfile(
                id = "123",
                name = "John Doe",
                email = "john@example.com",
                emergencyContact = "012-3456789"
            ),
            isLoading = false,
            error = null,
            onUpdateProfile = { _, _, _ -> },
            onLogout = {},
            onNavigateToSettings = {}
        )
    }
}
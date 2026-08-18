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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel


//profile screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ViewModel,
    onNavigateToSettings: () -> Unit
){

    val profile by viewModel.userProfile.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(profile?.name ?: "") }
    var emailInput by remember { mutableStateOf(profile?.email ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics{ contentDescription = " Open app settings button " }
                    ){
                        Icon(Icons.Default.Settings, contentDescription = null )
                    }
                }
            )
        }
    ){ padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if(isEditing){
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth().semantics{ contentDescription = " Edit name field " }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().semantics{ contentDescription = " Edit email field " }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        profile?.let{viewModel.updateProfile(it.id, nameInput, emailInput)}
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth().semantics{ contentDescription = " Save profile changes button " }
                ){
                    Text("Save Profile")
                }
            }else{
                Text(text = profile?.name ?: "Guest User" , style = MaterialTheme.typography.headlineMedium)
                Text(text =  profile?.email ?: "No email set ",style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        nameInput = profile?.name ?: ""
                        emailInput = profile?.email ?: ""
                        isEditing = true
                    },
                    modifier = Modifier.fillMaxWidth().semantics{ contentDescription = " Edit profile details button " }
                ){
                    Text("Edit Profile")
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {viewModel.logout()},
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().semantics{ contentDescription = " Log out account button " }
            ){
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }
        }
    }
}

//Settings screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAccessibility: () -> Unit,
    onNavigateToStarred:() -> Unit,
    onNavigateToHistory:() -> Unit,
){
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") })}
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()){
            ListItem(
                headlineContent = { Text("Accessibility Settings") },
                supportingContent = { Text("Visual mode, voice guidance, wheelchair, stroller options") },
                leadingIcon = { Icon(Icons.Default.AccessibilityNew, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToAccessibility() }
                    .semantics{ contentDescription = " Navigate to accessibility settings " }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Starred & Saved Locations") },
                supportingContent = { Text("Manage your saved locations") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToStarred() }
                    .semantics{ contentDescription = " Navigate to saved locations screen " }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Route History") },
                supportingContent = { Text("View previously traveled paths") },
                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToHistory() }
                    .semantics{ contentDescription = " Navigate to route history screen " }
            )
        }
    }
}

//Accesaibilty settings screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(viewModel: ViewModel){
    val settings by viewModel.accessibilitySettings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Accessibility Preferences") })}
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ){
            AccessibilityToggleRow(
                title = "Visual Assistance Mode ",
                description = "Enables high contract and extra screen reader datail",
                checked = settings.visualAssistanceMode,
                onCheckedChange = { viewModel.toggleVisualAssistance(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccessibilityToggleRow(
                title = "Voice Guidance",
                description = "Turn-by-turn spoken prompts during navigation",
                checked = settings.voiceGuidanceEnabled,
                onCheckedChange = { viewModel.toggleVoiceGuidance(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccessibilityToggleRow(
                title = "Wheelchair Accessible Mode",
                description = " Prioritizes routes with elevators and ramps ",
                checked = settings.wheelchairModeEnabled,
                onCheckedChange = { viewModel.toogleWheelchairMode(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccessibilityToggleRow(
                title = "Stroller Friendly Mode",
                description = " Avoids staris and narrow pathways ",
                checked = settings.strollerModeEnabled,
                onCheckedChange = { viewModel.toogleStrollerMode(it) }
            )

        }
    }
}

@Composable
fun AccessibilityToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true ) {
                contentDescription = " $title switch is currently ${if(checked) "on" else "off" }.$description "
            } ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

// starred locations screen

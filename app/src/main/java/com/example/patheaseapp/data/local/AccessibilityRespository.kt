package com.example.patheaseapp.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccessibilityPreferences(
    val visualAssistanceMode: Boolean = false,
    val voiceGuidanceEnabled: Boolean = true,
    val wheelchairAccessEnabled: Boolean = false,
    val strollerModeEnabled: Boolean = false,
)

class AccessibilityRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE)

    private val _accessibiilityStatus = MutableStateFlow(loadPreferences())
    val accessibilityStatus: StateFlow<AccessibilityPreferences> = _accessibiilityStatus.asStateFlow()

    private fun loadPreferences():AccessibilityPreferences {
        return AccessibilityPreferences(
            visualAssistanceMode = prefs.getBoolean("visual_assistance", false),
            voiceGuidanceEnabled = prefs.getBoolean("voice_guidance", true),
            wheelchairAccessEnabled = prefs.getBoolean("wheelchair_mode", false),
            strollerModeEnabled = prefs.getBoolean("stroller_mode", false),

        )
    }

}

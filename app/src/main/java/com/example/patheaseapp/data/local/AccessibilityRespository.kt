package com.example.patheaseapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccessibilityPreferences(
    val visualAssistanceMode: Boolean = false,
    val voiceGuidanceEnabled: Boolean = true,
    val wheelchairAccessEnabled: Boolean = false,
    val strollerModeEnabled: Boolean = false,
    val blindModeEnabled: Boolean = false,
    val keepScreenOn: Boolean = false,
)

class AccessibilityRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE)

    private val _accessibilityState = MutableStateFlow(loadPreferences())
    val accessibilityState: StateFlow<AccessibilityPreferences> = _accessibilityState.asStateFlow()

    private fun loadPreferences():AccessibilityPreferences {
        return AccessibilityPreferences(
            visualAssistanceMode = prefs.getBoolean("visual_assistance", false),
            voiceGuidanceEnabled = prefs.getBoolean("voice_guidance", true),
            wheelchairAccessEnabled = prefs.getBoolean("wheelchair_mode", false),
            strollerModeEnabled = prefs.getBoolean("stroller_mode", false),
            blindModeEnabled = prefs.getBoolean("Blind_mode", false),
            keepScreenOn = prefs.getBoolean("keep_screen_on", false),
        )
    }

    fun updatePreferences(transform:(AccessibilityPreferences)->AccessibilityPreferences){
        val updated = transform(_accessibilityState.value)
        _accessibilityState.value = updated

        prefs.edit {
            putBoolean("visual_assistance", updated.visualAssistanceMode)
            putBoolean("voice_guidance", updated.voiceGuidanceEnabled)
            putBoolean("wheelchair_mode", updated.wheelchairAccessEnabled)
            putBoolean("stroller_mode", updated.strollerModeEnabled)
            putBoolean("Blind_Mode", updated.blindModeEnabled)
            putBoolean("keep_screen_on", updated.keepScreenOn)
        }

    }


}

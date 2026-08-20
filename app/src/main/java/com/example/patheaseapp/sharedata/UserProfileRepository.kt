package com.example.patheaseapp.sharedata

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// TEMPORARY dummy repository — simulates the Profile module's emergency contact data.
// Replace internals once teammate's Profile module is ready (interface stays the same).
object UserProfileRepository {
    private val _emergencyContact = MutableStateFlow("012-345 6189") // dummy number for testing

    val emergencyContact: StateFlow<String> = _emergencyContact.asStateFlow()

    // Teammate's Profile screen will eventually call this when user edits their contact.
    fun setEmergencyContact(number: String) {
        _emergencyContact.value = number
    }
}
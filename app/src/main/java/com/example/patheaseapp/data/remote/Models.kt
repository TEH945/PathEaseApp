package com.example.patheaseapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SupabaseProfile(
    val id: String,
    val name: String,
    val email: String,
)

@Serializable
data class SupabaseStartedLocation(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val address: String,
)

@Serializable
data class RouteHistoryItem(
    val id: String,
    val origin: String,
    val destination: String,
    val timestamp: String,
)
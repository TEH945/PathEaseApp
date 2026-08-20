package com.example.patheaseapp.Hazard

data class Hazard(
    val id: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Float = 0f
)
package com.example.patheaseapp.Hazard

// Data layer for hazards — provides and stores hazard data (mock list for now, swappable for Firestore later).
class HazardRepository {

    private val mockHazards = mutableListOf(
        Hazard(id = "1", type = "Bumpy Road", lat = 1.4927, lng = 103.7414),
        Hazard(id = "2", type = "Barrier", lat = 1.4930, lng = 103.7420)
    )

    suspend fun getHazards(): List<Hazard> {
        return mockHazards.toList()
    }

    suspend fun addHazard(hazard: Hazard) {
        mockHazards.add(hazard)
    }

    suspend fun removeHazard(id: String) {
        mockHazards.removeAll { it.id == id }
    }
}
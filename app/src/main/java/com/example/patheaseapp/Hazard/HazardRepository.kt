package com.example.patheaseapp.Hazard

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// Only the fields we actually insert — keeps distanceMeters (client-only) out of the payload.
@Serializable
private data class NewHazard(
    val type: String,
    val lat: Double,
    val lng: Double,
    @SerialName("photo_url") val photoUrl: String?,
    @SerialName("reported_by") val reportedBy: String?
)

// Data layer for hazards — now backed by Supabase (Postgres + Storage)
// instead of an in-memory mock list, so reports are visible to every user.
class HazardRepository {

    private val table = hazardSupabaseClient.postgrest.from("hazards")
    private val bucket = hazardSupabaseClient.storage.from("hazard-photos")

    // Returns only currently-active hazards (not yet confirmed removed).
    suspend fun getHazards(): List<Hazard> {
        return table.select(columns = Columns.ALL) {
            filter { eq("is_active", true) }
        }.decodeList<Hazard>()
    }

    suspend fun addHazard(type: String, lat: Double, lng: Double, photoUrl: String?, reportedBy: String?) {
        table.insert(
            NewHazard(type = type, lat = lat, lng = lng, photoUrl = photoUrl, reportedBy = reportedBy)
        )
    }

    // Uploads a photo (raw bytes) to Storage, returns its public URL.
    suspend fun uploadPhoto(bytes: ByteArray): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        bucket.upload(fileName, bytes)
        return bucket.publicUrl(fileName)
    }

    // Called when another user taps "this hazard has been removed."
    suspend fun confirmRemoved(hazardId: String, currentCount: Int) {
        val newCount = currentCount + 1
        table.update(
            {
                set("confirmed_removed_count", newCount)
                if (newCount >= 3) set("is_active", false) // auto-deactivate after 3 confirmations
            }
        ) {
            filter { eq("id", hazardId) }
        }
    }
}
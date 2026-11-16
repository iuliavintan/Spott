package com.example.spott.map

import com.example.spott.drafts.DraftInput
import com.example.spott.drafts.ParkingReportDraft

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }

    companion object {
        val Timisoara = GeoPoint(45.7489, 21.2087)
    }
}

data class SelectedLocation(val point: GeoPoint, val address: String = "") {
    fun toDraftInput(existing: DraftInput = DraftInput()) = existing.copy(
        location = address.ifBlank { existing.location },
        latitude = point.latitude.toString(),
        longitude = point.longitude.toString()
    )
}

fun ParkingReportDraft.mapPointOrNull(): GeoPoint? {
    val lat = latitude ?: return null
    val lon = longitude ?: return null
    return if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) GeoPoint(lat, lon) else null
}

package com.example.spott.drafts

/** Unpublished local work. This is neither a saved car position nor a published report. */
data class ParkingReportDraft(
    val id: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

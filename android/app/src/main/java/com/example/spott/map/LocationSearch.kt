package com.example.spott.map

data class PlaceSuggestion(val id: String, val title: String, val subtitle: String)

/** One interaction owns one token, shared by predictions and the selected-place details call. */
interface PlaceSearchSession {
    suspend fun suggestions(query: String, bias: GeoPoint): List<PlaceSuggestion>
    suspend fun resolve(placeId: String): SelectedLocation
}

interface LocationSearch {
    val available: Boolean
    fun startSession(): PlaceSearchSession
}

fun interface AddressResolver {
    suspend fun address(point: GeoPoint): String?
}

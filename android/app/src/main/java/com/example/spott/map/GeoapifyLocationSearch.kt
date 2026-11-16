package com.example.spott.map

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.coroutineContext

class GeoapifyLocationSearch(
    private val apiKey: String,
    private val request: (String) -> String = ::requestGeoapify
) : LocationSearch {
    override val available = apiKey.isNotBlank()
    override fun startSession(): PlaceSearchSession = object : PlaceSearchSession {
        private val locations = mutableMapOf<String, SelectedLocation>()

        override suspend fun suggestions(query: String, bias: GeoPoint): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
            check(available) { "Search is not configured" }
            val text = URLEncoder.encode(query, "UTF-8")
            val key = URLEncoder.encode(apiKey, "UTF-8")
                val json = request("https://api.geoapify.com/v1/geocode/autocomplete?text=$text&format=json&limit=5&bias=proximity:${bias.longitude},${bias.latitude}&apiKey=$key")
                coroutineContext.ensureActive()
                val results = JSONObject(json).optJSONArray("results") ?: return@withContext emptyList()
                val found = mutableMapOf<String, SelectedLocation>()
                val suggestions = buildList {
                    for (index in 0 until results.length()) {
                        val item = results.getJSONObject(index)
                        val lat = item.optDouble("lat", Double.NaN)
                        val lon = item.optDouble("lon", Double.NaN)
                        val point = runCatching { GeoPoint(lat, lon) }.getOrNull() ?: continue
                        val address = item.optString("formatted")
                        val id = item.optString("place_id").ifBlank { "$lat,$lon:$address" }
                        found[id] = SelectedLocation(point, address)
                        add(PlaceSuggestion(id, item.optString("name").ifBlank { address }, address))
                    }
                }
                synchronized(locations) { locations.clear(); locations.putAll(found) }
                suggestions.distinctBy { it.id }
        }

        override suspend fun resolve(placeId: String): SelectedLocation = synchronized(locations) {
            checkNotNull(locations[placeId]) { "Search result is no longer available" }
        }
    }
}

private fun requestGeoapify(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 5_000
        connection.readTimeout = 8_000
        check(connection.responseCode == 200) { "Location search failed" }
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally { connection.disconnect() }
}

class AndroidAddressResolver(private val context: Context) : AddressResolver {
    @Suppress("DEPRECATION")
    override suspend fun address(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        Geocoder(context, Locale.getDefault()).getFromLocation(point.latitude, point.longitude, 1)
            ?.firstOrNull()?.getAddressLine(0)
    }
}

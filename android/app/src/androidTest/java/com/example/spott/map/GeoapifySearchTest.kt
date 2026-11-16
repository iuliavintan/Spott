package com.example.spott.map

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GeoapifySearchTest {
    @Test
    fun autocompleteUsesBiasAndResolvesReturnedCoordinatesWithoutAnotherRequest() = runTest {
        var requests = 0
        val provider = GeoapifyLocationSearch("test-key") { url ->
            requests++
            assertTrue(url.contains("bias=proximity:21.0,45.0"))
            assertTrue(url.contains("text=Test+street"))
            """{"results":[{"place_id":"test","formatted":"Test street","lat":46.77,"lon":23.59}]}"""
        }
        val session = provider.startSession()
        val suggestions = session.suggestions("Test street", GeoPoint(45.0, 21.0))
        assertEquals("Test street", suggestions.single().title)
        assertEquals(GeoPoint(46.77, 23.59), session.resolve(suggestions.single().id).point)
        assertEquals(1, requests)
    }

    @Test
    fun invalidCoordinatesAreNotTurnedIntoFakeLocations() = runTest {
        val provider = GeoapifyLocationSearch("test-key") {
            """{"results":[{"place_id":"bad","formatted":"Missing"},{"lat":91,"lon":21}]}"""
        }
        assertTrue(provider.startSession().suggestions("Test", GeoPoint.Timisoara).isEmpty())
        assertFalse(GeoapifyLocationSearch("").available)
    }
}

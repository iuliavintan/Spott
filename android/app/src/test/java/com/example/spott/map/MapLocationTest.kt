package com.example.spott.map

import com.example.spott.drafts.DraftInput
import com.example.spott.drafts.DraftValidator
import com.example.spott.drafts.ParkingReportDraft
import org.junit.Assert.*
import org.junit.Test

class MapLocationTest {
    @Test
    fun markersUseOnlyValidStoredCoordinatesWithoutGuessingAddresses() {
        val good = ParkingReportDraft("1", "Street", 45.0, 21.0, "", 1, 1)
        assertEquals(GeoPoint(45.0, 21.0), good.mapPointOrNull())
        assertNull(good.copy(latitude = null, longitude = null).mapPointOrNull())
        assertNull(good.copy(latitude = null).mapPointOrNull())
        assertNull(good.copy(latitude = Double.NaN).mapPointOrNull())
        assertNull(good.copy(longitude = 181.0).mapPointOrNull())
    }

    @Test
    fun selectionPrefillsRealCoordinatesAndKeepsExistingNotes() {
        val input = SelectedLocation(GeoPoint(45.75, 21.23), "University").toDraftInput(DraftInput(notes = "Keep this"))
        val value = DraftValidator.validate(input).value!!
        assertEquals("University", value.location)
        assertEquals(45.75, value.latitude!!, 0.0)
        assertEquals(21.23, value.longitude!!, 0.0)
        assertEquals("Keep this", value.notes)
    }
}

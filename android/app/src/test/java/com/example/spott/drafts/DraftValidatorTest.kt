package com.example.spott.drafts

import org.junit.Assert.*
import org.junit.Test

class DraftValidatorTest {
    @Test
    fun blankLocationIsRejected() {
        val result = DraftValidator.validate(DraftInput(location = "  "))
        assertEquals("Enter a location or address.", result.errors.location)
        assertNull(result.value)
    }

    @Test
    fun coordinatesMustBeSuppliedTogether() {
        assertNotNull(DraftValidator.validate(DraftInput("Library", latitude = "45")).errors.longitude)
        assertNotNull(DraftValidator.validate(DraftInput("Library", longitude = "21")).errors.latitude)
    }

    @Test
    fun onlyFiniteCoordinatesWithinGeographicRangesAreAccepted() {
        listOf("91", "-91", "NaN", "Infinity", "north", "45,2").forEach {
            assertNotNull(it, DraftValidator.validate(DraftInput("Library", it, "21")).errors.latitude)
        }
        listOf("181", "-181", "NaN", "Infinity", "east").forEach {
            assertNotNull(it, DraftValidator.validate(DraftInput("Library", "45", it)).errors.longitude)
        }
        val boundary = DraftValidator.validate(DraftInput(" Library ", "-90", "180", " note ")).value!!
        assertEquals(-90.0, boundary.latitude!!, 0.0)
        assertEquals(180.0, boundary.longitude!!, 0.0)
        assertEquals("Library", boundary.location)
        assertEquals("note", boundary.notes)
        assertEquals(90.0, DraftValidator.validate(DraftInput("A", "90", "-180")).value!!.latitude!!, 0.0)
        assertNull(DraftValidator.validate(DraftInput("A")).value!!.latitude)
    }
}

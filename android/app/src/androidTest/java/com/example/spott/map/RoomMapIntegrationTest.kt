package com.example.spott.map

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.spott.drafts.DraftDatabase
import com.example.spott.drafts.DraftInput
import com.example.spott.drafts.RoomParkingDraftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RoomMapIntegrationTest {
    @Test
    fun realRoomChangesMoveAndRemoveMarkersAndReopenRestoresTheirPositions() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "room-map-${UUID.randomUUID()}.db"
        var database = Room.databaseBuilder(context, DraftDatabase::class.java, name).build()
        val store = ViewModelStore()
        try {
            var repository = RoomParkingDraftRepository(database.draftDao())
            val id = repository.create(DraftInput("Library", "45", "21"))
            repository.create(DraftInput("Text only"))
            val noSearch = object : LocationSearch {
                override val available = false
                override fun startSession(): PlaceSearchSession = error("No network in storage tests")
            }
            val model = ParkingMapViewModel(repository, noSearch, AddressResolver { null }, SavedStateHandle())
            store.put("map", model)
            assertEquals(listOf(id), model.state.first { !it.loading }.markers.map { it.id })
            repository.update(id, DraftInput("Moved", "46", "22"))
            assertEquals(GeoPoint(46.0, 22.0), model.state.first { it.markers.firstOrNull()?.latitude == 46.0 }.markers.single().mapPointOrNull())
            withContext(Dispatchers.Main) { store.clear() }
            database.close()
            database = Room.databaseBuilder(context, DraftDatabase::class.java, name).build()
            repository = RoomParkingDraftRepository(database.draftDao())
            val reopened = ParkingMapViewModel(repository, noSearch, AddressResolver { null }, SavedStateHandle())
            store.put("reopened", reopened)
            assertEquals(GeoPoint(46.0, 22.0), reopened.state.first { !it.loading }.markers.single().mapPointOrNull())
            repository.delete(id)
            val afterDelete = reopened.state.first { !it.loading && it.markers.isEmpty() }
            assertEquals(1, afterDelete.drafts.size)
            assertEquals("Text only", afterDelete.drafts.single().location)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            database.close()
            context.deleteDatabase(name)
        }
    }
}

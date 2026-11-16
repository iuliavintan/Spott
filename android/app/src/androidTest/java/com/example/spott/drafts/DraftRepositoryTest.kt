package com.example.spott.drafts

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DraftRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "draft-test-${UUID.randomUUID()}.db"
    private lateinit var database: DraftDatabase
    private lateinit var repository: RoomParkingDraftRepository
    private val clock = Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC)

    @Before
    fun open() {
        database = Room.databaseBuilder(context, DraftDatabase::class.java, name).build()
        repository = RoomParkingDraftRepository(database.draftDao(), clock)
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun createdDraftCanBeReadThroughRepository() = runTest {
        val id = repository.create(DraftInput(" Library ", notes = " Side street "))
        val draft = repository.observeDraft(id).first()!!
        assertEquals("Library", draft.location)
        assertEquals("Side street", draft.notes)
        assertEquals(1000L, draft.createdAt)
        assertEquals(listOf(draft), repository.observeDrafts().first())
    }

    @Test
    fun editingPreservesIdentityAndCreationTime() = runTest {
        val id = repository.create(DraftInput("Library"))
        val later = RoomParkingDraftRepository(database.draftDao(), Clock.fixed(Instant.ofEpochMilli(2000), ZoneOffset.UTC))
        assertTrue(later.update(id, DraftInput("Campus", "45", "21", "Updated")))
        val draft = later.observeDraft(id).first()!!
        assertEquals(id, draft.id)
        assertEquals(1000L, draft.createdAt)
        assertEquals(2000L, draft.updatedAt)
        assertEquals("Campus", draft.location)
        assertEquals(1, later.observeDrafts().first().size)
    }

    @Test
    fun deletedDraftCannotBeReadOrRecreatedByEditing() = runTest {
        val id = repository.create(DraftInput("Library"))
        repository.delete(id)
        assertNull(repository.observeDraft(id).first())
        assertFalse(repository.update(id, DraftInput("Campus")))
        assertTrue(repository.observeDrafts().first().isEmpty())
        repository.delete(id) // Repeated deletion is harmless.
    }

    @Test
    fun storedDraftSurvivesClosingAndReopeningDatabase() = runTest {
        val id = repository.create(DraftInput("Persistent location", "-90", "180", "Keep me"))
        database.close()
        open()
        val row = database.draftDao().observeById(id).first()!!
        assertEquals("Persistent location", row.location)
        assertEquals("Keep me", row.notes)
        assertEquals(-90.0, row.latitude!!, 0.0)
        assertEquals(180.0, row.longitude!!, 0.0)
        assertEquals(id, repository.observeDrafts().first().single().id)
    }

    @Test
    fun observersReceiveNewDraftsAndNewestDraftAppearsFirst() = runTest {
        repository.create(DraftInput("First"))
        val observation = async(start = CoroutineStart.UNDISPATCHED) { repository.observeDrafts().first { it.size == 2 } }
        val later = RoomParkingDraftRepository(database.draftDao(), Clock.fixed(Instant.ofEpochMilli(2000), ZoneOffset.UTC))
        later.create(DraftInput("Second"))
        assertEquals(listOf("Second", "First"), observation.await().map { it.location })
    }

    @Test
    fun invalidInputCannotChangeStoredData() = runTest {
        val id = repository.create(DraftInput("Original"))
        try {
            repository.update(id, DraftInput("", "91", "21"))
            fail("Invalid update should fail")
        } catch (_: InvalidDraftException) { }
        try {
            repository.create(DraftInput("Other", "45"))
            fail("Unpaired coordinates should fail")
        } catch (_: InvalidDraftException) { }
        assertEquals("Original", repository.observeDraft(id).first()!!.location)
        assertEquals(1, repository.observeDrafts().first().size)
    }

    @Test
    fun movingDeviceClockBackwardDoesNotMoveModificationBeforeCreation() = runTest {
        val id = repository.create(DraftInput("Library"))
        val earlier = RoomParkingDraftRepository(database.draftDao(), Clock.fixed(Instant.ofEpochMilli(500), ZoneOffset.UTC))
        earlier.update(id, DraftInput("Campus"))
        assertEquals(1000L, repository.observeDraft(id).first()!!.updatedAt)
    }
}

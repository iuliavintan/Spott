package com.example.spott.drafts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID

interface ParkingDraftRepository {
    fun observeDrafts(): Flow<List<ParkingReportDraft>>
    fun observeDraft(id: String): Flow<ParkingReportDraft?>
    suspend fun create(input: DraftInput): String
    suspend fun update(id: String, input: DraftInput): Boolean
    suspend fun delete(id: String)
}

class InvalidDraftException(val errors: DraftErrors) : IllegalArgumentException("Invalid draft")

class RoomParkingDraftRepository(
    private val dao: DraftDao,
    private val clock: Clock = Clock.systemUTC()
) : ParkingDraftRepository {
    override fun observeDrafts() = dao.observeAll().map { rows -> rows.map { it.toDraft() } }
    override fun observeDraft(id: String) = dao.observeById(id).map { it?.toDraft() }

    override suspend fun create(input: DraftInput): String {
        val value = validate(input)
        val id = UUID.randomUUID().toString()
        val now = clock.millis()
        dao.insert(DraftEntity(id, value.location, value.latitude, value.longitude, value.notes, now, now))
        return id
    }

    override suspend fun update(id: String, input: DraftInput): Boolean {
        val value = validate(input)
        return dao.update(id, value.location, value.latitude, value.longitude, value.notes, clock.millis()) == 1
    }

    override suspend fun delete(id: String) = dao.delete(id)

    private fun validate(input: DraftInput): ValidatedDraft {
        val result = DraftValidator.validate(input)
        return result.value ?: throw InvalidDraftException(result.errors)
    }
}

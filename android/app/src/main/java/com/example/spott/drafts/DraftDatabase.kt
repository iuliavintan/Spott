package com.example.spott.drafts

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "parking_report_drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDraft() = ParkingReportDraft(id, location, latitude, longitude, notes, createdAt, updatedAt)
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM parking_report_drafts ORDER BY updatedAt DESC, id ASC")
    fun observeAll(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM parking_report_drafts WHERE id = :id")
    fun observeById(id: String): Flow<DraftEntity?>

    @Insert
    suspend fun insert(draft: DraftEntity)

    // An UPDATE cannot recreate a deleted draft and never changes its identity or creation time.
    @Query("""UPDATE parking_report_drafts SET location = :location, latitude = :latitude,
        longitude = :longitude, notes = :notes, updatedAt = MAX(createdAt, updatedAt, :updatedAt)
        WHERE id = :id""")
    suspend fun update(id: String, location: String, latitude: Double?, longitude: Double?, notes: String, updatedAt: Long): Int

    @Query("DELETE FROM parking_report_drafts WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [DraftEntity::class], version = 1, exportSchema = true)
abstract class DraftDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao
}

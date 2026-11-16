package com.example.spott

import android.app.Application
import androidx.room.Room
import com.example.spott.drafts.DraftDatabase
import com.example.spott.drafts.ParkingDraftRepository
import com.example.spott.drafts.RoomParkingDraftRepository

class SpottApplication : Application() {
    // One database for the process; the application context cannot retain an Activity.
    val draftRepository: ParkingDraftRepository by lazy {
        val database = Room.databaseBuilder(this, DraftDatabase::class.java, "spott-drafts.db").build()
        RoomParkingDraftRepository(database.draftDao())
    }
}

package com.example.spott

import android.app.Application
import androidx.room.Room
import com.example.spott.drafts.DraftDatabase
import com.example.spott.drafts.ParkingDraftRepository
import com.example.spott.drafts.RoomParkingDraftRepository
import com.example.spott.map.AndroidAddressResolver
import com.example.spott.map.GeoapifyLocationSearch

class SpottApplication : Application() {
    val mapsConfigured get() = true
    val locationSearch by lazy { GeoapifyLocationSearch(getString(R.string.geoapify_api_key)) }
    val addressResolver by lazy { AndroidAddressResolver(this) }
    // One database for the process; the application context cannot retain an Activity.
    val draftRepository: ParkingDraftRepository by lazy {
        val database = Room.databaseBuilder(this, DraftDatabase::class.java, "spott-drafts.db").build()
        RoomParkingDraftRepository(database.draftDao())
    }
}

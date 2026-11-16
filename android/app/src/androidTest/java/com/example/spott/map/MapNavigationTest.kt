package com.example.spott.map

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.spott.drafts.DraftDatabase
import com.example.spott.drafts.RoomParkingDraftRepository
import com.example.spott.ui.drafts.SpottApp
import com.example.spott.ui.theme.SpottTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** The only fake is Google's boundary. Navigation, form validation and Room are real. */
class MapNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "navigation-${UUID.randomUUID()}.db"
    private val database = Room.databaseBuilder(context, DraftDatabase::class.java, databaseName).build()

    @After fun cleanup() { database.close(); context.deleteDatabase(databaseName) }

    @Test
    fun searchSelectionReusesCrudAndShowOnMapWithoutAnApiKey() {
        val fakeSearch = object : LocationSearch {
            override val available = true
            override fun startSession() = object : PlaceSearchSession {
                override suspend fun suggestions(query: String, bias: GeoPoint) = listOf(PlaceSuggestion("test-place", "Central square", "Test city"))
                override suspend fun resolve(placeId: String) = SelectedLocation(GeoPoint(46.77, 23.59), "Central square")
            }
        }
        compose.setContent { SpottTheme { SpottApp(RoomParkingDraftRepository(database.draftDao()), fakeSearch, AddressResolver { null }, false) } }
        compose.onNodeWithText("Search places").performClick()
        compose.onNode(hasText("Search places") and hasSetTextAction()).performTextInput("Central")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Central square").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Central square").performClick()
        compose.onNodeWithText("Save parking draft").performScrollTo().performClick()
        compose.onNodeWithText("Location / address").assertTextContains("Central square")
        compose.onNodeWithText("Notes (optional)").performTextInput("Near the entrance")
        compose.onNodeWithText("Save draft").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("View details").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("View details").performScrollTo().performClick()
        compose.onNodeWithText("Coordinates: 46.77, 23.59").assertIsDisplayed()
        compose.onNodeWithText("Edit draft").performScrollTo().performClick()
        compose.onNodeWithText("Coordinates (advanced)").performScrollTo().performClick()
        compose.onNodeWithText("Latitude").performScrollTo().performTextReplacement("46.78")
        compose.onNodeWithText("Save draft").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Show on map").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Show on map").performScrollTo().performClick()
        compose.onNodeWithText("View details").performScrollTo().performClick()
        compose.onNodeWithText("Coordinates: 46.78, 23.59").assertIsDisplayed()
        compose.onNodeWithText("Delete draft").performScrollTo().performClick()
        compose.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("New draft").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Saved drafts").performClick()
        compose.onNodeWithText("Central square").assertDoesNotExist()
    }
}

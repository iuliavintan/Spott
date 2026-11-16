package com.example.spott.drafts

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.spott.MainActivity
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class DraftUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun validationAndActivityRecreationKeepUnsavedInput() {
        compose.onNodeWithText("New draft").performClick()
        compose.onNodeWithText("Save draft").performScrollTo().performClick()
        compose.onNodeWithText("Enter a location or address.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Location / address").performTextInput("Unsaved university location")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Location / address").assertTextContains("Unsaved university location")
        compose.onNodeWithText("Coordinates (advanced)").performScrollTo().performClick()
        compose.onNodeWithText("Latitude").performScrollTo().performTextInput("91")
        compose.onNodeWithText("Save draft").performScrollTo().performClick()
        compose.onNodeWithText("Enter latitude from -90 to 90 using a decimal dot.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Enter longitude too.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
    }

    @Test
    fun userCanCreateEditAndDeleteDraft() {
        val location = "UI test ${UUID.randomUUID()}"
        compose.onNodeWithText("New draft").performClick()
        compose.onNodeWithText("Location / address").performTextInput(location)
        compose.onNodeWithText("Save draft").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Edit draft").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(location).assertIsDisplayed()
        compose.onNodeWithText("Edit draft").performClick()
        compose.onNodeWithText("Location / address").performTextReplacement("$location edited")
        compose.onNodeWithText("Save draft").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Edit draft").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("$location edited").assertIsDisplayed()
        compose.onNodeWithText("Delete draft").performScrollTo().performClick()
        compose.onNodeWithText("Keep draft").performClick()
        compose.onNodeWithText("$location edited").assertIsDisplayed()
        compose.onNodeWithText("Delete draft").performScrollTo().performClick()
        compose.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("New draft").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("$location edited").assertDoesNotExist()
    }
}

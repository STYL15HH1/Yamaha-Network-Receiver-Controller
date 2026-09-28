package com.styl15hh1.rn301controller

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
class MaintenanceUiTest {
    @get:Rule val compose = createComposeRule()
    private var expanded by mutableStateOf(false)
    @Test fun sourcesCollapseExpandAndSelectUsingExistingGrid() {
        var selected: String? = null
        compose.setContent { ReceiverTheme(true) {
            ExpandableSources(listOf(Source("CD"), Source("OPTICAL")), Source("CD"), true, expanded, { expanded = it }) { selected = it }
        } }
        compose.onNodeWithText("Optical").assertDoesNotExist()
        compose.onNodeWithContentDescription("Expand sources").performClick()
        compose.onNodeWithText("Optical").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("OPTICAL", selected) }
        compose.onNodeWithContentDescription("Collapse sources").performClick()
        compose.onNodeWithText("Optical").assertDoesNotExist()
    }
    @Test fun reenteringHomeRetainsControlledExpansion() {
        var visible by mutableStateOf(true)
        compose.setContent { ReceiverTheme(true) {
            if (visible) ExpandableSources(listOf(Source("CD")), null, true, expanded, { expanded = it }) {}
        } }
        compose.onNodeWithContentDescription("Expand sources").performClick()
        compose.onNodeWithText("CD").assertExists()
        compose.runOnIdle { visible = false }; compose.waitForIdle()
        compose.runOnIdle { visible = true }
        compose.onNodeWithText("CD").assertExists()
    }
    @Test fun disconnectedExpandedSourcesCannotBeSelected() {
        var called = false
        compose.setContent { ReceiverTheme(true) {
            ExpandableSources(listOf(Source("CD")), null, false, expanded, { expanded = it }) { called = true }
        } }
        compose.onNodeWithContentDescription("Expand sources").performClick()
        compose.onNodeWithText("CD").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertFalse(called) }
    }
    @Test fun toolbarPowerUsesCurrentStateAndBusyDisablesAction() {
        var state by mutableStateOf(ReceiverStatus(connectionState=ConnectionState.CONNECTED, powerState=PowerState.ON))
        var busy by mutableStateOf(false)
        val requests = mutableListOf<Boolean>()
        compose.setContent { ReceiverTheme(true) { PowerAction(state, busy) { requests += it; busy = true } } }
        compose.onNodeWithContentDescription("Standby").performClick()
        compose.onNodeWithContentDescription("Standby").assertIsNotEnabled().performClick()
        compose.runOnIdle {
            assertEquals(listOf(false), requests)
            state = state.copy(powerState=PowerState.STANDBY); busy = false
        }
        compose.onNodeWithContentDescription("Power on").performClick()
        compose.runOnIdle { assertEquals(listOf(false, true), requests) }
    }
    @Test fun disconnectedToolbarPowerIsDisabled() {
        compose.setContent { ReceiverTheme(true) { PowerAction(ReceiverStatus(), false) { error("Unexpected command") } } }
        compose.onNodeWithContentDescription("Power on").assertIsNotEnabled()
    }
}

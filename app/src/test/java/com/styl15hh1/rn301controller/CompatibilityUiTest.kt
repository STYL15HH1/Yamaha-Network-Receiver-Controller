package com.styl15hh1.rn301controller

import android.app.Application
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
class CompatibilityUiTest {
    @get:Rule val compose = createComposeRule()
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val commands = mutableListOf<YamahaCommand>()
    private val repo = YamahaRepository(object : YamahaTransport {
        override suspend fun resolve(address: ReceiverAddress) = "192.168.1.55"
        override suspend fun description(ip: String): String = throw YamahaException(ReceiverError(ErrorKind.RECEIVER_UNAVAILABLE))
        override suspend fun command(ip: String, command: YamahaCommand): String {
            commands.add(command)
            return when (command) {
                YamahaCommand.Config -> configXml
                YamahaCommand.Status -> statusXml()
                else -> throw YamahaException(ReceiverError(ErrorKind.UNSUPPORTED_COMMAND))
            }
        }
    }, object : AddressStore {
        override suspend fun read() = "192.168.1.55"
        override suspend fun write(address: String) = error("Report must not save address")
    }, io = Dispatchers.Unconfined)
    private val settings = AppSettings(MemorySettingsStore())
    private val vm = ReceiverViewModel(repo, settings = settings)
    private fun render() {
        compose.setContent { MaterialTheme {
            val page by vm.page.collectAsState()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (page == ReceiverPage.COMPATIBILITY) CompatibilityScreen(vm) else SettingsScreen(vm, settings) {}
            }
        } }
    }
    @Test fun settingsNavigationIsPassiveUntilExplicitGenerateAndBackReturnsToSettings() {
        render()
        compose.onNodeWithText("Compatibility Report").performScrollTo().performClick()
        compose.onNodeWithText("Generate Report").assertExists()
        compose.runOnIdle { assertTrue(commands.isEmpty()); assertEquals(ReceiverPage.COMPATIBILITY, vm.page.value); vm.back() }
        compose.runOnIdle { assertEquals(ReceiverPage.SETTINGS, vm.page.value) }
    }
    @Test fun copyAndShareUseSameSanitizedPlainText() {
        render()
        compose.onNodeWithText("Compatibility Report").performScrollTo().performClick()
        compose.onNodeWithText("Generate Report").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Copy Report").performScrollTo().performClick()
        var copied = ""
        compose.runOnIdle {
            copied = application.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString()
            assertTrue(copied.contains("R-N301")); assertFalse(copied.contains("192.168.1.55"))
            assertTrue(commands.all { YamahaXmlBuilder.build(it).contains("cmd=\"GET\"") })
        }
        compose.onNodeWithText("Share Report").performScrollTo().performClick()
        compose.runOnIdle {
            val chooser = shadowOf(application).nextStartedActivity
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
            assertEquals(Intent.ACTION_SEND, send.action); assertEquals("text/plain", send.type)
            assertEquals(copied, send.getStringExtra(Intent.EXTRA_TEXT))
        }
    }
}

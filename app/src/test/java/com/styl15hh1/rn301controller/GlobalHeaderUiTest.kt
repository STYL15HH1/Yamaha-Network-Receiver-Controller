package com.styl15hh1.rn301controller

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="en-rUS-w411dp-h891dp")
class GlobalHeaderUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun oneReceiverHeaderRemainsAcrossAllFeatureRoutes() {
        val server=ServerFake().apply {source="CD"}
        val fake=object:YamahaTransport by server {
            override suspend fun command(ip:String,command:YamahaCommand):String=when(command) {
                YamahaCommand.NetRadioList -> radioListXml()
                YamahaCommand.NetRadioInfo -> radioInfoXml()
                else -> server.command(ip,command)
            }
        }
        val store=object:AddressStore {override suspend fun read()="";override suspend fun write(address:String)=Unit}
        val repo=YamahaRepository(fake,store,io=kotlinx.coroutines.Dispatchers.Unconfined)
        runBlocking {repo.connect("192.168.1.55")}
        lateinit var vm:ReceiverViewModel
        compose.setContent {
            vm=androidx.compose.runtime.remember {ReceiverViewModel(repo)}
            MaterialTheme {ReceiverScreen(vm,vm.settings) {}}
        }
        fun header() {
            org.junit.Assert.assertEquals(1,compose.onAllNodesWithText("R-N301").fetchSemanticsNodes().count {it.boundsInRoot.top < 64f})
            org.junit.Assert.assertTrue(compose.onAllNodesWithText("Connected",substring=true).fetchSemanticsNodes().any {it.boundsInRoot.top < 64f})
            compose.onAllNodesWithContentDescription("Settings").assertCountEquals(1)
            compose.onAllNodesWithContentDescription("Standby").assertCountEquals(1)
            compose.onNodeWithContentDescription("Standby").assertIsEnabled()
            if(vm.page.value==ReceiverPage.HOME) compose.onNodeWithContentDescription("App Home").assertDoesNotExist()
            else compose.onNodeWithContentDescription("App Home").assertIsDisplayed()
        }
        header()
        compose.runOnIdle {vm.openTuner()}
        compose.waitUntil(5000) {org.robolectric.shadows.ShadowLooper.idleMainLooper();!vm.busy.value}
        header()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        compose.runOnIdle {vm.settings()}
        header()
        compose.runOnIdle {vm.about()}
        header()
        for(source in listOf("SERVER","NET RADIO")) {
            compose.runOnIdle {vm.home();vm.source(source)}
            compose.waitUntil(5000) {org.robolectric.shadows.ShadowLooper.idleMainLooper();!vm.busy.value && vm.page.value==ReceiverPage.BROWSER}
            header()
            compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        }
        compose.runOnIdle {vm.openPlayer()}
        compose.waitUntil(5000) {org.robolectric.shadows.ShadowLooper.idleMainLooper();!vm.busy.value}
        header()
        compose.onNodeWithContentDescription("App Home").performClick()
        compose.runOnIdle {org.junit.Assert.assertEquals(ReceiverPage.HOME,vm.page.value)}
        header()
        compose.runOnIdle {vm.foreground(false)}
    }
}

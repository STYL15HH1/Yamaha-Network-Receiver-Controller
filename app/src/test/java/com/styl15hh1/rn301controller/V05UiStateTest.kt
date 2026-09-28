package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V05UiStateTest {
    @Test fun artworkIsWhiteTransparentAndDisabledPolicyRemainsShared() {
        for(name in listOf("spotify_wordmark","airplay_artwork")) {
            val image=ImageIO.read(File("src/main/res/drawable-nodpi/$name.png"))
            var transparent=0;var opaque=0
            for(y in 0 until image.height) for(x in 0 until image.width) {
                val pixel=image.getRGB(x,y);val alpha=pixel ushr 24
                if(alpha==0) transparent++ else { assertEquals(0xFFFFFF,pixel and 0xFFFFFF); if(alpha==255) opaque++ }
            }
            assertTrue(transparent>image.width*image.height/4);assertTrue(opaque>100)
        }
        assertEquals(R.drawable.airplay_artwork,sourceDrawable("AirPlay"))
        assertEquals(0.38f,ControlPolicy.contentAlpha(false),0f)
        assertEquals(1f,ControlPolicy.contentAlpha(true),0f)
    }
    @Test fun allVectorIconsHaveVisiblePathsIncludingSettings() {
        File("src/main/res/drawable").listFiles()!!.filter { it.extension=="xml" }.forEach { file ->
            val doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            if(doc.documentElement.tagName=="vector") {
                val paths=doc.getElementsByTagName("path")
                assertTrue(file.name,paths.length>0)
                assertTrue(file.name,(0 until paths.length).any {
                    !paths.item(it).attributes.getNamedItem("android:pathData").nodeValue.isNullOrBlank()
                })
            }
        }
    }
    @Test fun systemDefaultAndAllNativeLanguageChoicesPersistAfterRestart() {
        val memory=mutableMapOf<String,String>()
        val store=object:SettingsStore {
            override fun read(key:String)=memory[key]
            override fun write(key:String,value:String){memory[key]=value}
        }
        assertEquals(listOf("system","en","es","de","fr","it","pl","ko","ja"),AppLanguages.choices)
        for(tag in AppLanguages.choices) {
            AppSettings(store).language(tag)
            assertEquals(tag,AppSettings(store).state.value.language)
        }
        AppSettings(store).language("system")
        assertEquals("system",AppSettings(store).state.value.language)
    }
    @Test fun settingsAboutAndBackNavigation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo=repository(ServerFake())
        val vm=ReceiverViewModel(repo)
        try {
            runCurrent();vm.settings();assertEquals(ReceiverPage.SETTINGS,vm.page.value)
            vm.about();assertEquals(ReceiverPage.ABOUT,vm.page.value)
            vm.back();assertEquals(ReceiverPage.SETTINGS,vm.page.value)
            vm.back();assertEquals(ReceiverPage.HOME,vm.page.value)
        } finally { vm.foreground(false);Dispatchers.resetMain() }
    }
    @Test fun browserSystemBackNavigatesBeforeLeavingRoot() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake=ServerFake().apply { layer=2 }
        val repo=repository(fake);val vm=ReceiverViewModel(repo)
        try {
            runCurrent();vm.source("SERVER");advanceUntilIdle()
            assertEquals(ReceiverPage.BROWSER,vm.page.value)
            vm.back();advanceUntilIdle()
            assertEquals(ReceiverPage.BROWSER,vm.page.value);assertEquals(1,repo.browser.value.list!!.layer)
            vm.back();advanceUntilIdle();assertEquals(ReceiverPage.HOME,vm.page.value)
        } finally { vm.foreground(false);Dispatchers.resetMain() }
    }
    @Test fun serverNowPlayingPollsWithoutPollingBrowserListsAndStopsInBackground() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fake=ServerFake();val repo=repository(fake);val vm=ReceiverViewModel(repo)
        try {
            runCurrent();vm.foreground(true);runCurrent()
            advanceTimeBy(4001);runCurrent()
            assertEquals(3,fake.commands.count { it==com.styl15hh1.rn301controller.data.protocol.YamahaCommand.ServerInfo })
            assertTrue(fake.commands.none { it==com.styl15hh1.rn301controller.data.protocol.YamahaCommand.ServerList })
            vm.foreground(false);val count=fake.commands.size
            advanceTimeBy(10000);runCurrent();assertEquals(count,fake.commands.size)
        } finally { vm.foreground(false);Dispatchers.resetMain() }
    }
    private suspend fun TestScope.repository(fake:ServerFake):YamahaRepository {
        val store=object:AddressStore { override suspend fun read()="";override suspend fun write(address:String)=Unit }
        return YamahaRepository(fake,store,io=StandardTestDispatcher(testScheduler)).also { it.connect("receiver") }
    }
}

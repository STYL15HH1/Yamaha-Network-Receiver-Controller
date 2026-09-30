package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.browser.*
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V110BrowserTest {
    private fun browser(fake: ServerFake, limit: Int = 64, timeout: Long = 90_000) =
        AggregatingMediaBrowser(ServerMediaBrowser(fake, YamahaXmlParser()), limit, timeout)

    @Test fun allServerPagesAggregateAndSelectionReturnsToOriginalWindow() = runTest {
        val fake=ServerFake().apply { max=17 }
        val b=browser(fake); val list=b.getCurrentList("receiver")
        assertEquals(17,list.items.size); assertTrue(list.aggregated)
        assertNull(list.page); assertNull(list.pages)
        assertEquals(2,fake.commands.filterIsInstance<YamahaCommand.ServerPage>().size)
        assertEquals(17,list.items.map { it.originPage to it.line }.distinct().size)
        fake.commands.clear()
        b.selectItem("receiver",list,list.items[9])
        assertTrue(fake.commands.contains(YamahaCommand.ServerPage(false)))
        assertTrue(fake.commands.contains(YamahaCommand.ServerSelect(2)))
        assertTrue(fake.commands.indexOf(YamahaCommand.ServerControl(PlayerAction.PLAY)) >
            fake.commands.indexOf(YamahaCommand.ServerSelect(2)))
        assertEquals(9,fake.current)
    }
    @Test fun repositoryPublishesAggregateAndRefreshesMetadataAfterTrackSelection() = runTest {
        val fake=ServerFake().apply { max=17 }
        val store=object:AddressStore { override suspend fun read()=""; override suspend fun write(address:String)=Unit }
        val repo=YamahaRepository(fake,store,io=StandardTestDispatcher(testScheduler))
        repo.connect("receiver");repo.openBrowser()
        assertEquals(17,repo.browser.value.list!!.items.size)
        repo.selectMedia(repo.browser.value.list!!.items[9])
        assertEquals("SERVER",repo.player.value.nowPlaying!!.source)
        assertEquals(BrowserPhase.CONTENT,repo.browser.value.phase)
    }
    @Test fun singlePageDoesNotPageAndBackRootRefreshRetainAggregation() = runTest {
        val fake=ServerFake().apply { layer=3 }
        val b=browser(fake)
        assertTrue(b.getCurrentList("receiver").aggregated)
        assertEquals(2,b.goBack("receiver").layer)
        assertTrue(b.goHome("receiver").root)
        assertTrue(b.getCurrentList("receiver").aggregated)
        assertTrue(fake.commands.none { it is YamahaCommand.ServerPage })
        assertEquals(2,fake.commands.count { it == YamahaCommand.ServerBack })
    }
    @Test fun emptyServerMenuDoesNotPage() = runTest {
        val fake=ServerFake().apply { max=0; current=0 }
        assertTrue(browser(fake).getCurrentList("receiver").items.isEmpty())
        assertTrue(fake.commands.none { it is YamahaCommand.ServerPage })
    }
    @Test fun laterInitialPageRewindsAndDuplicateNamesKeepSeparateIdentities() = runTest {
        val fake=ServerFake().apply { max=17; current=17 }
        val list=browser(fake).getCurrentList("receiver")
        assertEquals(17,list.items.size)
        assertEquals(1,list.items.first().originPage)
        assertEquals(3,list.items.last().originPage)
        assertEquals(4,fake.commands.filterIsInstance<YamahaCommand.ServerPage>().size)
        assertTrue(list.items.count { it.title == "Track" } > 1)
    }
    @Test fun staleServerWindowCannotSelectWrongTrack() = runTest {
        val fake=ServerFake().apply { max=17 }; val b=browser(fake)
        val list=b.getCurrentList("receiver");fake.changedNames=true;fake.commands.clear()
        try { b.selectItem("receiver",list,list.items[9]);fail("Expected stale menu") }
        catch(e:BrowserException) { assertEquals(BrowserFailure.CHANGED,e.failure) }
        assertTrue(fake.commands.none { it is YamahaCommand.ServerSelect })
    }
    @Test fun serverPageLimitFailsBeforePaging() = runTest {
        val fake=ServerFake().apply { max=17 }
        try { browser(fake,limit=2).getCurrentList("receiver");fail("Expected limit") }
        catch(e:BrowserException) { assertEquals(BrowserFailure.LIMIT_REACHED,e.failure) }
        assertTrue(fake.commands.none { it is YamahaCommand.ServerPage })
    }
    @Test fun serverStuckPageTerminates() = runTest {
        val fake=ServerFake().apply { max=17;stuckPage=true }
        try { browser(fake).getCurrentList("receiver");fail("Expected changed page") }
        catch(e:BrowserException) { assertEquals(BrowserFailure.CHANGED,e.failure) }
        assertEquals(1,fake.commands.filterIsInstance<YamahaCommand.ServerPage>().size)
    }
    @Test fun serverAggregationTimeoutDoesNotPublishPartialList() = runTest {
        val fake=ServerFake().apply { max=17;listDelay=1000 }
        try { browser(fake,timeout=1500).getCurrentList("receiver");fail("Expected timeout") }
        catch(e:BrowserException) { assertEquals(BrowserFailure.TIMEOUT,e.failure) }
        assertEquals(1500,testScheduler.currentTime)
    }
    @Test fun appHomeOnlyNavigatesWithoutChangingReceiverOrPlayer() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fake=ServerFake()
            val store=object:AddressStore { override suspend fun read()="";override suspend fun write(address:String)=Unit }
            val repo=YamahaRepository(fake,store,io=StandardTestDispatcher(testScheduler))
            repo.connect("receiver");repo.showPlayer(true);repo.refresh()
            val vm=ReceiverViewModel(repo);runCurrent()
            vm.source("SERVER");advanceUntilIdle()
            assertEquals(ReceiverPage.BROWSER,vm.page.value)
            val status=repo.status.value;val player=repo.player.value;val browser=repo.browser.value
            fake.commands.clear();vm.home();runCurrent()
            assertEquals(ReceiverPage.HOME,vm.page.value)
            assertTrue(fake.commands.isEmpty())
            assertEquals(status,repo.status.value);assertEquals(player,repo.player.value)
            assertEquals(browser,repo.browser.value)
            vm.foreground(false)
        } finally { Dispatchers.resetMain() }
    }
}

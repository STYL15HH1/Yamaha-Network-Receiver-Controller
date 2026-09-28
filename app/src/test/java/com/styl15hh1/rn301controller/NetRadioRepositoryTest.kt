package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetRadioRepositoryTest {
    private suspend fun TestScope.repo(f:NetRadioFake):YamahaRepository {
        val store=object:AddressStore { override suspend fun read()="";override suspend fun write(address:String)=Unit }
        return YamahaRepository(f,store,io=StandardTestDispatcher(testScheduler)).also{it.connect("receiver")}
    }
    @Test fun activationOpensNetRadioAdapterAndTracksPath()=runTest {
        val f=NetRadioFake().apply{source="CD"};val r=repo(f)
        r.selectSource("NET RADIO");r.openBrowser("NET RADIO")
        assertEquals("NET RADIO",r.browser.value.source);assertEquals(BrowserPhase.CONTENT,r.browser.value.phase)
        assertTrue(f.commands.none{it==YamahaCommand.ServerList})
        r.selectMedia(r.browser.value.list!!.items.single())
        assertEquals(listOf("Locations"),r.browser.value.navigationPath)
    }
    @Test fun stationTraversalRefreshesGenericPlayerAndFavorite()=runTest {
        val f=NetRadioFake();val r=repo(f)
        r.navigateRadioPath(listOf("Locations","Europe","Radio"))
        assertEquals("NET RADIO",r.player.value.nowPlaying!!.source)
        assertEquals("Radio",r.browser.value.selectedStation!!.displayName)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
    }
    @Test fun commandFailureIsIsolatedFromConnectionAndDoesNotResolveAgain()=runTest {
        val f=NetRadioFake();val r=repo(f);r.openBrowser("NET RADIO")
        f.fault=ErrorKind.UNSUPPORTED_COMMAND
        r.refreshBrowser()
        assertEquals(BrowserFailure.LOAD_FAILED,r.browser.value.error)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
        assertNull(r.status.value.error)
        f.fault=null;r.refresh();assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
    }
    @Test fun serviceFailureDoesNotChangeReceiverState()=runTest {
        val f=NetRadioFake().apply{menuStatus="Unavailable"};val r=repo(f);r.openBrowser("NET RADIO")
        assertEquals(BrowserFailure.SERVICE_UNAVAILABLE,r.browser.value.error)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
        assertNull(r.status.value.error)
    }
    @Test fun timeoutIsBrowserErrorWithClearedPath()=runTest {
        val f=NetRadioFake().apply{listDelay=100_000};val r=repo(f);r.openBrowser("NET RADIO")
        assertEquals(BrowserFailure.TIMEOUT,r.browser.value.error)
        assertNull(r.browser.value.navigationPath)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
    }
    @Test fun stopReadsBackActualStateAndUnsupportedSkipNeverWrites()=runTest {
        val f=NetRadioFake();val r=repo(f)
        r.controlPlayer(PlayerAction.STOP)
        assertEquals(PlaybackState.STOPPED,r.player.value.nowPlaying!!.playback)
        f.commands.clear();r.controlPlayer(PlayerAction.NEXT)
        assertTrue(f.commands.none{it is YamahaCommand.NetRadioControl})
        assertEquals(ErrorKind.UNSUPPORTED_COMMAND,r.player.value.error!!.kind)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
    }
    @Test fun sourceChangeInvalidatesPathAndBlocksBrowserSelection()=runTest {
        val f=NetRadioFake();val r=repo(f);r.openBrowser("NET RADIO")
        val item=r.browser.value.list!!.items.single();f.source="CD";f.commands.clear()
        r.selectMedia(item)
        assertEquals(BrowserFailure.INACTIVE,r.browser.value.error)
        assertNull(r.browser.value.navigationPath)
        assertTrue(f.commands.none{it is YamahaCommand.NetRadioSelect})
    }
    @Test fun viewModelBackReturnsReceiverMenuBeforeLeavingBrowser()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=NetRadioFake();val r=repo(f);val vm=ReceiverViewModel(r)
        try {
            runCurrent();vm.source("NET RADIO");advanceUntilIdle()
            assertEquals(ReceiverPage.BROWSER,vm.page.value)
            vm.selectMedia(r.browser.value.list!!.items.single());advanceUntilIdle()
            vm.back();advanceUntilIdle()
            assertEquals(ReceiverPage.BROWSER,vm.page.value);assertTrue(r.browser.value.list!!.root)
            vm.back();advanceUntilIdle();assertEquals(ReceiverPage.HOME,vm.page.value)
        }finally{vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun foregroundPollsPlayerButNotListsAndBackgroundCancelsPolling()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f=NetRadioFake();val r=repo(f);val vm=ReceiverViewModel(r)
        try {
            runCurrent();vm.foreground(true);runCurrent();advanceTimeBy(4001);runCurrent()
            assertEquals(3,f.commands.count{it==YamahaCommand.NetRadioInfo})
            assertTrue(f.commands.none{it==YamahaCommand.NetRadioList})
            vm.foreground(false);val count=f.commands.size;advanceTimeBy(10000);runCurrent()
            assertEquals(count,f.commands.size)
        }finally{vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun cancellationClearsLoadingAndDoesNotDisconnect()=runTest {
        val f=NetRadioFake().apply{busyReads=100};val r=repo(f)
        val job=launch{r.openBrowser("NET RADIO")};runCurrent();job.cancelAndJoin()
        assertEquals(BrowserPhase.IDLE,r.browser.value.phase)
        assertNull(r.browser.value.navigationPath)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
    }
}

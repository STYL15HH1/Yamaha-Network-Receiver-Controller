package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.data.browser.*
import com.styl15hh1.rn301controller.data.player.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

internal class ServerFake : YamahaTransport {
    val commands = mutableListOf<YamahaCommand>()
    var source = "SERVER"
    var layer = 1
    var current = 1
    var max = 2
    var stuckPage = false
    var changedNames = false
    var busyReads = 0
    var stuck = false
    var fault: ErrorKind? = null
    var listDelay = 0L
    var acceptedInput = true
    var readsBeforeInput = 0
    var pendingSource: String? = null
    override suspend fun resolve(address: ReceiverAddress) = "192.168.1.55"
    override suspend fun description(ip: String) = """<Unit_Description Unit_Name="R-N301"/>"""
    override suspend fun command(ip: String, command: YamahaCommand): String {
        commands += command
        return when(command) {
            YamahaCommand.Config -> configXml
            YamahaCommand.Status -> {
                if (pendingSource != null && readsBeforeInput-- <= 0) { source = pendingSource!!; pendingSource = null }
                statusXml(source=source)
            }
            is YamahaCommand.Input -> { if (acceptedInput) pendingSource = command.id; ack }
            YamahaCommand.ServerInfo -> serverInfoXml()
            YamahaCommand.ServerList -> {
                fault?.let { throw YamahaException(ReceiverError(it)) }
                delay(listDelay)
                mediaListXml(layer,current,if(max == 0) 0 else max,
                    if(max == 0) "" else (1..minOf(8, max - current + 1)).joinToString("") { line ->
                        val title = if (changedNames) "Changed" else if (line == 1) "Folder $layer" else "Track"
                        val attribute = if (line == 1) "Container" else "Item"
                        "<Line_$line><Txt>$title</Txt><Attribute>$attribute</Attribute></Line_$line>"
                    },
                    if(busyReads-- > 0) "Busy" else "Ready", "Menu $layer")
            }
            is YamahaCommand.ServerSelect -> { if(command.line == 1) { layer++; current=1 }; ack }
            YamahaCommand.ServerBack -> { if(!stuck) layer = (layer-1).coerceAtLeast(1); current=1; ack }
            is YamahaCommand.ServerPage -> { if (!stuckPage) current += if(command.next) 8 else -8; ack }
            else -> ack
        }
    }
    private val ack = """<YAMAHA_AV rsp="PUT" RC="0"/>"""
}
@OptIn(ExperimentalCoroutinesApi::class)
class ServerRepositoryTest {
    private val store = object : AddressStore {
        override suspend fun read() = ""
        override suspend fun write(address: String) = Unit
    }
    private suspend fun TestScope.repo(fake: ServerFake) = YamahaRepository(fake,store,
        io=StandardTestDispatcher(testScheduler)).also { it.connect("receiver") }
    @Test fun openListOnlyAfterConfirmedSource() = runTest {
        val fake = ServerFake().apply { source="CD"; readsBeforeInput=2 }
        val repo=repo(fake); repo.selectSource("SERVER"); repo.openBrowser()
        val listIndex=fake.commands.indexOf(YamahaCommand.ServerList)
        assertTrue(listIndex > 0)
        assertEquals("SERVER",repo.status.value.currentSource?.id)
        assertEquals(BrowserPhase.CONTENT,repo.browser.value.phase)
    }
    @Test fun noBrowseWhenSourceDoesNotSwitch() = runTest {
        val fake=ServerFake().apply { source="CD";acceptedInput=false }
        val repo=repo(fake); repo.selectSource("SERVER");repo.openBrowser()
        assertTrue(fake.commands.none { it==YamahaCommand.ServerList })
        assertEquals(BrowserFailure.INACTIVE,repo.browser.value.error)
    }
    @Test fun emptyServerListDoesNotDisconnect() = runTest {
        val fake=ServerFake().apply { max=0;current=0 }
        val repo=repo(fake);repo.openBrowser()
        assertEquals(BrowserPhase.EMPTY,repo.browser.value.phase)
        assertEquals(ConnectionState.CONNECTED,repo.status.value.connectionState)
    }
    @Test fun folderSelectionNavigatesWithoutPlayback() = runTest {
        val fake=ServerFake();val repo=repo(fake);repo.openBrowser();fake.commands.clear()
        repo.selectMedia(repo.browser.value.list!!.items.first())
        assertEquals(2,repo.browser.value.list!!.layer)
        assertEquals(1,fake.commands.count { it is YamahaCommand.ServerSelect })
        assertTrue(fake.commands.none { it is YamahaCommand.ServerControl })
    }
    @Test fun trackSelectionUsesSelectThenPlayAndReadsActualMetadata() = runTest {
        val fake=ServerFake();val repo=repo(fake);repo.openBrowser();fake.commands.clear()
        repo.selectMedia(repo.browser.value.list!!.items[1])
        val select=fake.commands.indexOf(YamahaCommand.ServerSelect(2))
        assertTrue(select>=0)
        assertTrue(fake.commands.indexOf(YamahaCommand.ServerControl(PlayerAction.PLAY)) > select)
        assertEquals("SERVER",repo.player.value.nowPlaying!!.source)
    }
    @Test fun externalMenuChangePreventsStaleSelection() = runTest {
        val fake=ServerFake();val repo=repo(fake);repo.openBrowser();fake.layer=3;fake.commands.clear()
        repo.selectMedia(repo.browser.value.list!!.items.first())
        assertEquals(BrowserFailure.CHANGED,repo.browser.value.error)
        assertTrue(fake.commands.none { it is YamahaCommand.ServerSelect })
    }
    @Test fun nextAndPreviousPageUseLocalLineIds() = runTest {
        val fake=ServerFake().apply { max=17 }
        val pages=ServerMediaBrowser(fake,YamahaXmlParser())
        var list=pages.getCurrentList("receiver")
        list=pages.changePage("receiver",list,true);assertEquals(2,list.page)
        list=pages.changePage("receiver",list,false);assertEquals(1,list.page)
    }
    @Test fun backDescendsOneLevelAndRootLeavesWithoutPut() = runTest {
        val fake=ServerFake().apply { layer=3 };val repo=repo(fake);repo.openBrowser()
        assertFalse(repo.browserBack());assertEquals(2,repo.browser.value.list!!.layer)
        assertFalse(repo.browserBack());assertEquals(1,repo.browser.value.list!!.layer)
        fake.commands.clear()
        assertTrue(repo.browserBack());assertTrue(fake.commands.none { it==YamahaCommand.ServerBack })
    }
    @Test fun homeReturnsRepeatedlyWithProgressChecks() = runTest {
        val fake=ServerFake().apply { layer=4 };val repo=repo(fake);repo.openBrowser()
        repo.browserHome();assertTrue(repo.browser.value.list!!.root)
        assertEquals(3,fake.commands.count { it==YamahaCommand.ServerBack })
    }
    @Test fun stuckHomeDoesNotLoopForever() = runTest {
        val fake=ServerFake().apply { layer=4;stuck=true };val repo=repo(fake);repo.openBrowser()
        repo.browserHome();assertEquals(BrowserFailure.CHANGED,repo.browser.value.error)
        assertEquals(1,fake.commands.count { it==YamahaCommand.ServerBack })
    }
    @Test fun busyRetriesAreFiniteAndEventDriven() = runTest {
        val fake=ServerFake().apply { busyReads=99 };val repo=repo(fake);repo.openBrowser()
        assertEquals(BrowserFailure.NOT_READY,repo.browser.value.error)
        assertEquals(6,fake.commands.count { it==YamahaCommand.ServerList })
        fake.commands.clear();repeat(3){repo.refresh()}
        assertTrue(fake.commands.none { it==YamahaCommand.ServerList })
    }
    @Test fun busyThenReadySucceeds() = runTest {
        val fake=ServerFake().apply { busyReads=2 };val repo=repo(fake);repo.openBrowser()
        assertEquals(BrowserPhase.CONTENT,repo.browser.value.phase)
        assertEquals(3,fake.commands.count { it==YamahaCommand.ServerList })
    }
    @Test fun mediaServerFailuresAreIsolatedFromReceiver() = runTest {
        for ((error,expected) in listOf(ErrorKind.TIMEOUT to BrowserFailure.TIMEOUT,
            ErrorKind.COMMAND_FAILED to BrowserFailure.LOAD_FAILED,ErrorKind.INVALID_RESPONSE to BrowserFailure.INVALID_RESPONSE)) {
            val fake=ServerFake().apply { fault=error };val repo=repo(fake);repo.openBrowser()
            assertEquals(expected,repo.browser.value.error)
            assertEquals(ConnectionState.CONNECTED,repo.status.value.connectionState)
            assertNull(repo.status.value.error)
        }
    }
    @Test fun overallBrowserDeadlineIsFinite() = runTest {
        val fake=ServerFake().apply { listDelay=100000 };val repo=repo(fake);repo.openBrowser()
        assertEquals(BrowserFailure.TIMEOUT,repo.browser.value.error)
        assertEquals(90000,testScheduler.currentTime)
    }
    @Test fun cancellingAfterSelectionDoesNotRepeatPut() = runTest {
        val fake=ServerFake();val repo=repo(fake);repo.openBrowser();fake.commands.clear()
        val job=launch { repo.selectMedia(repo.browser.value.list!!.items.first()) }
        runCurrent()
        job.cancelAndJoin()
        assertEquals(1,fake.commands.count { it is YamahaCommand.ServerSelect })
        assertEquals(BrowserPhase.IDLE,repo.browser.value.phase)
        assertEquals(ConnectionState.CONNECTED,repo.status.value.connectionState)
        repo.refreshBrowser()
        assertEquals(2,repo.browser.value.list!!.layer)
        assertEquals(1,fake.commands.count { it is YamahaCommand.ServerSelect })
    }
    @Test fun externalSourceChangeClearsOldMetadataEvenWhenPlayerHidden() = runTest {
        val fake=ServerFake();val repo=repo(fake);repo.showPlayer(true);repo.refresh()
        assertEquals("SERVER",repo.player.value.nowPlaying!!.source)
        repo.showPlayer(false);fake.source="Spotify";repo.refresh()
        assertNull(repo.player.value.nowPlaying)
    }
    @Test fun serverPlayerUsesItsOwnCommands() = runTest {
        val fake=ServerFake();val player=LegacyYamahaPlayer(fake,YamahaXmlParser())
        for(action in PlayerAction.entries) player.control("192.168.1.55","SERVER",action)
        assertEquals(PlayerAction.entries.map { YamahaCommand.ServerControl(it) },fake.commands)
        assertEquals("SERVER",player.getNowPlaying("192.168.1.55","SERVER").source)
    }
}

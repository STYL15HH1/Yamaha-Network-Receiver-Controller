package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.discovery.*
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartupTest {
    private class Store(var saved: String) : AddressStore {
        override suspend fun read() = saved
        override suspend fun write(address: String) { saved = address }
    }
    private class Fake(var online: Boolean = true) : YamahaTransport {
        var resolutions = 0
        override suspend fun resolve(address: ReceiverAddress): String {
            resolutions++
            if (!online) throw YamahaException(ReceiverError(ErrorKind.TIMEOUT))
            return "192.168.1.55"
        }
        override suspend fun description(ip: String) = "<Unit_Description Unit_Name=\"R-N301\"/>"
        override suspend fun command(ip: String, command: YamahaCommand) = when (command) {
            YamahaCommand.Config -> configXml
            else -> statusXml()
        }
    }
    @Test fun firstLaunchScansWithoutConnectingToFoundDevice() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val fake = Fake()
        val repo = YamahaRepository(fake, Store(""), io = dispatcher)
        var scans = 0
        val vm = ReceiverViewModel(repo, YamahaDiscovery {
            scans++
            DiscoveryState(DiscoveryPhase.FOUND, listOf(DiscoveredReceiver("192.168.1.55", "R-N301", "Receiver", "uuid:test")))
        })
        try {
            vm.foreground(true); runCurrent()
            assertEquals(1, scans)
            assertEquals(0, fake.resolutions)
            assertEquals(DiscoveryPhase.FOUND, vm.discoveryState.value.phase)
            assertEquals(ConnectionState.DISCONNECTED, vm.status.value.connectionState)
        } finally { vm.foreground(false); Dispatchers.resetMain() }
    }
    @Test fun savedReceiverReconnectsBeforeDiscovery() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val fake = Fake()
        var scans = 0
        val vm = ReceiverViewModel(YamahaRepository(fake, Store("receiver"), io = dispatcher),
            YamahaDiscovery { scans++; DiscoveryState(DiscoveryPhase.EMPTY) })
        try {
            vm.foreground(true); runCurrent()
            assertEquals(1, fake.resolutions)
            assertEquals(0, scans)
            assertEquals(ConnectionState.CONNECTED, vm.status.value.connectionState)
        } finally { vm.foreground(false); Dispatchers.resetMain() }
    }
    @Test fun failedSavedReceiverOffersDiscovery() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = Store("receiver")
        var scans = 0
        val vm = ReceiverViewModel(YamahaRepository(Fake(false), store, io = dispatcher),
            YamahaDiscovery { scans++; DiscoveryState(DiscoveryPhase.EMPTY) })
        try {
            vm.foreground(true); runCurrent()
            assertEquals(1, scans)
            assertEquals("receiver", store.saved)
            assertEquals(ConnectionState.UNAVAILABLE, vm.status.value.connectionState)
        } finally { vm.foreground(false); Dispatchers.resetMain() }
    }
    @Test fun explicitReconnectTriesSavedReceiverAndPreservesFeaturePageOnSuccess()=runTest {
        val dispatcher=StandardTestDispatcher(testScheduler);Dispatchers.setMain(dispatcher)
        val f=Fake();var scans=0
        val vm=ReceiverViewModel(YamahaRepository(f,Store("receiver"),io=dispatcher),
            YamahaDiscovery {scans++;DiscoveryState(DiscoveryPhase.EMPTY)})
        try {
            runCurrent();vm.settings();repeat(3){vm.reconnect()};runCurrent()
            assertEquals(1,f.resolutions);assertEquals(0,scans)
            assertEquals(ConnectionState.CONNECTED,vm.status.value.connectionState)
            assertEquals(ReceiverPage.SETTINGS,vm.page.value)
        } finally {vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun failedReconnectOpensExistingDiscoveryWithoutAutoSelectingCandidate()=runTest {
        val dispatcher=StandardTestDispatcher(testScheduler);Dispatchers.setMain(dispatcher)
        val f=Fake(false);var scans=0;val store=Store("receiver")
        val vm=ReceiverViewModel(YamahaRepository(f,store,io=dispatcher),
            YamahaDiscovery {scans++;DiscoveryState(DiscoveryPhase.FOUND,listOf(DiscoveredReceiver("192.168.1.56","R-N301","Other","uuid:other")))})
        try {
            runCurrent();vm.reconnect();runCurrent()
            assertEquals(1,f.resolutions);assertEquals(1,scans)
            assertEquals(ReceiverPage.CONNECTION,vm.page.value)
            assertEquals("receiver",store.saved)
        } finally {vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun explicitConnectWithNoSavedReceiverOnlyScans()=runTest {
        val dispatcher=StandardTestDispatcher(testScheduler);Dispatchers.setMain(dispatcher)
        val f=Fake();var scans=0
        val vm=ReceiverViewModel(YamahaRepository(f,Store(""),io=dispatcher),
            YamahaDiscovery {scans++;DiscoveryState(DiscoveryPhase.EMPTY)})
        try {
            runCurrent();vm.reconnect();runCurrent()
            assertEquals(0,f.resolutions);assertEquals(1,scans)
            assertEquals(ReceiverPage.CONNECTION,vm.page.value)
        } finally {vm.foreground(false);Dispatchers.resetMain()}
    }
    @Test fun backgroundCancelsScan() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        var cancelled = false
        val vm = ReceiverViewModel(YamahaRepository(Fake(), Store(""), io = dispatcher), YamahaDiscovery {
            try { delay(60000); DiscoveryState(DiscoveryPhase.EMPTY) }
            finally { cancelled = true }
        })
        try {
            vm.foreground(true); runCurrent()
            assertEquals(DiscoveryPhase.SEARCHING, vm.discoveryState.value.phase)
            vm.foreground(false); runCurrent()
            assertTrue(cancelled)
            assertEquals(DiscoveryPhase.IDLE, vm.discoveryState.value.phase)
        } finally { vm.foreground(false); Dispatchers.resetMain() }
    }
}

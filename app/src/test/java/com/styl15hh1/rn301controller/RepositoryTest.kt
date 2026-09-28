package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTest {
    private class Store : AddressStore {
        var value = ""
        override suspend fun read() = value
        override suspend fun write(address: String) { value = address }
    }
    private class Fake : YamahaTransport {
        val commands = mutableListOf<YamahaCommand>()
        var fault: ErrorKind? = null
        var config = configXml
        var basic = statusXml()
        var concurrent = 0
        var maximumConcurrent = 0
        override suspend fun resolve(address: ReceiverAddress) = "192.168.1.100"
        override suspend fun description(ip: String) = "<Unit_Description Unit_Name=\"R-N301\"/>"
        override suspend fun command(ip: String, command: YamahaCommand): String {
            concurrent++
            maximumConcurrent = maxOf(maximumConcurrent, concurrent)
            try {
                delay(10)
                commands.add(command)
                fault?.let { throw YamahaException(ReceiverError(it)) }
                return when (command) {
                    YamahaCommand.Config -> config
                    YamahaCommand.Status -> basic
                    else -> "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>"
                }
            } finally { concurrent-- }
        }
    }

    @Test fun connectAndPersist() = runTest {
        val fake = Fake(); val store = Store()
        val repo = YamahaRepository(fake, store, io = StandardTestDispatcher(testScheduler))
        repo.connect("192.168.1.100")
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertEquals("R-N301", repo.status.value.modelName)
        assertEquals("192.168.1.100", store.value)
    }
    @Test fun timeoutIsUnavailableAndRecovers() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver.local")
        fake.fault = ErrorKind.TIMEOUT
        repo.refresh()
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
        assertTrue(repo.status.value.stale)
        repo.refresh()
        repo.refresh()
        assertEquals(PowerState.UNAVAILABLE, repo.status.value.powerState)
        assertNull(repo.status.value.volume)
        assertEquals(ErrorKind.TIMEOUT, repo.status.value.error!!.kind)
        fake.fault = null
        repo.refresh()
        assertEquals(PowerState.ON, repo.status.value.powerState)
        assertEquals(ConnectionState.CONNECTED, repo.status.value.connectionState)
    }
    @Test fun wrongModelRejectedWithoutWrites() = runTest {
        val fake = Fake().apply { config = configXml.replace("R-N301", "RX-V475") }
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        assertEquals(ErrorKind.UNSUPPORTED_DEVICE, repo.status.value.error!!.kind)
        assertTrue(fake.commands.none { it is YamahaCommand.Power })
    }
    @Test fun immediateReadAfterWriteAndNativeStep() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        repo.adjustVolume(1)
        assertEquals(YamahaCommand.SetVolume(Volume(26, 0, "")), fake.commands[fake.commands.lastIndex - 1])
        assertEquals(YamahaCommand.Status, fake.commands.last())
        assertEquals(25, repo.status.value.volume!!.value) // actual receiver read, not optimistic 26
    }
    @Test fun invalidVolumeNeverSent() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        val count = fake.commands.size
        repo.setVolume(101)
        assertEquals(count, fake.commands.size)
        assertEquals(ErrorKind.UNSUPPORTED_COMMAND, repo.status.value.error!!.kind)
    }
    @Test fun requestsSerialized() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        coroutineScope {
            launch { repo.refresh() }
            launch { repo.power(true) }
            launch { repo.toggleMute() }
        }
        assertEquals(1, fake.maximumConcurrent)
    }
    @Test fun failedReconnectCannotControlPreviousReceiver() = runTest {
        val fake = Fake()
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        repo.connect("http://bad")
        val count = fake.commands.size
        repo.power(true)
        repo.refresh()
        assertEquals(count, fake.commands.size)
        assertEquals(PowerState.UNAVAILABLE, repo.status.value.powerState)
    }
    @Test fun standbyDisablesVolumeWrites() = runTest {
        val fake = Fake().apply { basic = statusXml(power = "Standby") }
        val repo = YamahaRepository(fake, Store(), io = StandardTestDispatcher(testScheduler))
        repo.connect("receiver")
        val count = fake.commands.size
        repo.adjustVolume(1)
        assertEquals(count, fake.commands.size)
    }
}

package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import com.styl15hh1.rn301controller.ui.ReceiverViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @Test fun foregroundPollingStopsAndRestarts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        var reads = 0
        val transport = object : YamahaTransport {
            override suspend fun resolve(address: ReceiverAddress) = "192.168.1.100"
            override suspend fun description(ip: String) = "<Unit_Description Unit_Name=\"R-N301\"/>"
            override suspend fun command(ip: String, command: YamahaCommand): String = when (command) {
                YamahaCommand.Config -> configXml
                YamahaCommand.Status -> { reads++; statusXml() }
                else -> "<YAMAHA_AV rsp=\"PUT\" RC=\"0\"/>"
            }
        }
        val store = object : AddressStore {
            override suspend fun read() = "receiver"
            override suspend fun write(address: String) = Unit
        }
        val repo = YamahaRepository(transport, store, io = dispatcher)
        val vm = ReceiverViewModel(repo)
        try {
            runCurrent()
            vm.connect()
            runCurrent()
            assertEquals(1, reads)
            vm.foreground(true)
            runCurrent()
            assertEquals(2, reads)
            advanceTimeBy(2001)
            runCurrent()
            assertEquals(3, reads)
            vm.foreground(false)
            advanceTimeBy(20000)
            runCurrent()
            assertEquals(3, reads)
            vm.foreground(true)
            runCurrent()
            assertEquals(4, reads)
            vm.connect()
            vm.foreground(false)
            assertFalse(vm.busy.value)
        } finally {
            vm.foreground(false)
            Dispatchers.resetMain()
        }
    }
}

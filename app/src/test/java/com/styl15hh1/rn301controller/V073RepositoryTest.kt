package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.repository.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

internal class Tuner073Fake:YamahaTransport {
    val commands=mutableListOf<YamahaCommand>()
    var band=TunerBand.FM
    var mode=FmMode.AUTO
    var am=1134
    var source="TUNER"
    var failPut=false
    var ignorePut=false
    var failConfig=false
    var standby=false
    private fun fixture(name:String)=javaClass.getResource("/$name")!!.readText()
    override suspend fun resolve(address:ReceiverAddress)="192.168.1.55"
    override suspend fun description(ip:String)="""<Unit_Description Unit_Name="R-N301"/>"""
    override suspend fun command(ip:String,command:YamahaCommand):String {
        commands+=command
        val put=command is YamahaCommand.SetBand || command is YamahaCommand.SetFmMode ||
            command is YamahaCommand.TuneAm || command is YamahaCommand.SeekAm
        if(put && failPut)throw YamahaException(ReceiverError(ErrorKind.COMMAND_FAILED))
        when(command) {
            YamahaCommand.Config -> {
                if(failConfig)throw YamahaException(ReceiverError(ErrorKind.TIMEOUT))
                return configXml.replace("</Config>","<Version>1.13/0.05</Version></Config>")
            }
            YamahaCommand.Status -> return statusXml(source=source,power=if(standby)"Standby" else "On")
            YamahaCommand.TunerConfig -> return fixture("rn301-tuner-config.xml")
            YamahaCommand.TunerPresets -> return fixture("rn301-presets.xml")
            YamahaCommand.TunerInfo -> return fixture("rn301-tuner-not-ready.xml").replace("Not Ready","Ready")
                .replace("<Band>FM","<Band>${band.name}").replace("<FM_Mode>Auto","<FM_Mode>${mode.wire}")
                .replace("<Current><Val>9740</Val><Exp>2</Exp><Unit>MHz</Unit></Current>",
                    if(band==TunerBand.AM)"<Current><Val>$am</Val><Exp>0</Exp><Unit>kHz</Unit></Current>"
                    else "<Current><Val>9740</Val><Exp>2</Exp><Unit>MHz</Unit></Current>")
            is YamahaCommand.SetBand -> if(!ignorePut)band=command.band
            is YamahaCommand.SetFmMode -> if(!ignorePut)mode=command.mode
            is YamahaCommand.TuneAm -> if(!ignorePut)am=command.value
            else -> Unit
        }
        return """<YAMAHA_AV rsp="PUT" RC="0"/>"""
    }
}
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class V073RepositoryTest {
    private suspend fun TestScope.repo(f:Tuner073Fake):YamahaRepository {
        val store=object:AddressStore{override suspend fun read()="";override suspend fun write(address:String)=Unit}
        return YamahaRepository(f,store,io=StandardTestDispatcher(testScheduler)).also {
            it.connect("receiver");it.showTuner(true);it.refresh()
        }
    }
    @Test fun switchBandsUsesReadbackAndPreservesPresets()=runTest {
        val f=Tuner073Fake();val r=repo(f);val presets=r.tuner.value.presets
        r.setBand(TunerBand.AM)
        assertEquals("AM",r.tuner.value.status?.band);assertEquals("1134 kHz",r.tuner.value.status?.frequency?.display)
        assertEquals(presets,r.tuner.value.presets)
        r.setBand(TunerBand.FM);assertEquals("FM",r.tuner.value.status?.band)
    }
    @Test fun amDirectStepAndSeekUseBandSpecificCommands()=runTest {
        val f=Tuner073Fake().apply{band=TunerBand.AM};val r=repo(f)
        r.tuneAm("1143");r.stepAm(-1);r.seekAm(true);r.seekAm(false)
        assertTrue(f.commands.contains(YamahaCommand.TuneAm(1143)))
        assertTrue(f.commands.contains(YamahaCommand.TuneAm(1134)))
        assertTrue(f.commands.contains(YamahaCommand.SeekAm(true)));assertTrue(f.commands.contains(YamahaCommand.SeekAm(false)))
        assertEquals(1134,r.tuner.value.status?.frequency?.value)
    }
    @Test fun offGridAmAndWrongBandNeverWrite()=runTest {
        val f=Tuner073Fake().apply{band=TunerBand.AM};val r=repo(f);f.commands.clear()
        r.tuneAm("1135");r.tuneFm("101.2");r.setFmMode(FmMode.MONO)
        assertTrue(f.commands.none{it is YamahaCommand.TuneAm || it is YamahaCommand.TuneFm || it is YamahaCommand.SetFmMode})
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
    }
    @Test fun modeAndBandAckAreNotOptimisticState()=runTest {
        val f=Tuner073Fake().apply{ignorePut=true};val r=repo(f)
        r.setFmMode(FmMode.MONO);assertEquals(FmMode.AUTO,r.tuner.value.status?.fmMode)
        r.setBand(TunerBand.AM);assertEquals("FM",r.tuner.value.status?.band)
    }
    @Test fun autoMonoReadsActualMode()=runTest {
        val f=Tuner073Fake();val r=repo(f)
        r.setFmMode(FmMode.MONO);assertEquals(FmMode.MONO,r.tuner.value.status?.fmMode)
        r.setFmMode(FmMode.AUTO);assertEquals(FmMode.AUTO,r.tuner.value.status?.fmMode)
    }
    @Test fun failedTunerWriteDoesNotDisconnect()=runTest {
        val f=Tuner073Fake();val r=repo(f);f.failPut=true;r.setBand(TunerBand.AM)
        assertEquals("FM",r.tuner.value.status?.band)
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState)
        assertEquals(ErrorContext.COMMAND,r.status.value.error?.context)
    }
    @Test fun firmwareFailureDoesNotAffectConnection()=runTest {
        val r=repo(Tuner073Fake().apply{failConfig=true})
        assertEquals(ConnectionState.CONNECTED,r.status.value.connectionState);assertNull(r.status.value.firmwareVersion)
    }
    @Test fun firmwareIsCachedWithoutExtraPollingRequests()=runTest {
        val f=Tuner073Fake();val r=repo(f)
        assertEquals("1.13/0.05",r.status.value.firmwareVersion);f.commands.clear();repeat(3){r.refresh()}
        assertTrue(f.commands.none{it==YamahaCommand.Config})
    }
    @Test fun inactiveOrStandbyTunerCannotWrite()=runTest {
        for(f in listOf(Tuner073Fake().apply{source="CD"},Tuner073Fake().apply{standby=true})) {
            val r=repo(f);f.commands.clear();r.setBand(TunerBand.AM)
            assertTrue(f.commands.none{it is YamahaCommand.SetBand})
        }
    }
}

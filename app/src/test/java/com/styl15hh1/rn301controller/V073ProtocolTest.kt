package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import org.junit.Assert.*
import org.junit.Test

class V073ProtocolTest {
    private val parser=YamahaXmlParser()
    private fun fixture(name:String)=javaClass.getResource("/$name")!!.readText()
    private fun body(c:YamahaCommand)=YamahaXmlBuilder.build(c).substringAfter("?>")
    private fun info()=fixture("rn301-tuner-not-ready.xml").replace("Not Ready","Ready")
    @Test fun exactBandCommands() {
        for(b in TunerBand.entries) assertEquals(
            """<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>${b.name}</Band></Tuning></Play_Control></Tuner></YAMAHA_AV>""",body(YamahaCommand.SetBand(b)))
    }
    @Test fun exactModeCommands() {
        for(m in FmMode.entries) assertEquals(
            """<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><FM_Mode>${m.wire}</FM_Mode></Play_Control></Tuner></YAMAHA_AV>""",body(YamahaCommand.SetFmMode(m)))
    }
    @Test fun exactAmCommands() {
        assertEquals("""<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Band>AM</Band><Freq><AM><Val>1134</Val><Exp>0</Exp><Unit>kHz</Unit></AM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>""",body(YamahaCommand.TuneAm(1134)))
        for(up in listOf(true,false)) assertEquals(
            """<YAMAHA_AV cmd="PUT"><Tuner><Play_Control><Tuning><Freq><AM><Val>Auto ${if(up) "Up" else "Down"}</Val></AM></Freq></Tuning></Play_Control></Tuner></YAMAHA_AV>""",body(YamahaCommand.SeekAm(up)))
        for(value in listOf(0,3001))assertThrows(IllegalArgumentException::class.java){body(YamahaCommand.TuneAm(value))}
    }
    @Test fun actualRegionalAmConfigAndFormatting() {
        val range=parser.tunerConfig(fixture("rn301-tuner-config.xml"),TunerBand.AM)!!
        assertEquals(TuningRange(531,1611,9,TunerBand.AM),range)
        assertEquals(1134,range.parse("1134"));assertNull(range.parse("1134.5"));assertNull(range.parse("1135"))
        assertNull(range.parse("1620"));assertEquals("9 kHz",range.stepDisplay)
        assertEquals(1143,range.stepFrom(ReceiverFrequency(1134,0,"kHz"),1))
        assertEquals(1125,range.stepFrom(ReceiverFrequency(1134,0,"kHz"),-1))
        assertNull(range.stepFrom(ReceiverFrequency(9740,2,"MHz"),1))
        assertEquals("1134 kHz",ReceiverFrequency(1134,0,"kHz").display)
        assertEquals("97.4 MHz",ReceiverFrequency(9740,2,"MHz").display)
    }
    @Test fun regionalAmTenKhzGridAndMissingConfig() {
        val range=TuningRange(530,1710,10,TunerBand.AM)
        assertEquals(1140,range.parse("1140"));assertNull(range.parse("1134"))
        assertNull(parser.tunerConfig("""<YAMAHA_AV rsp="GET" RC="0"><Tuner><Config/></Tuner></YAMAHA_AV>""",TunerBand.AM))
        val bad=fixture("rn301-tuner-config.xml").replace("<Val>9</Val>","<Val>0</Val>")
        assertThrows(YamahaException::class.java){parser.tunerConfig(bad,TunerBand.AM)}
        assertNotNull(parser.tunerConfig(bad,TunerBand.FM))
    }
    @Test fun modeIsIndependentOfSignal() {
        val fm=parser.tuner(info().replace("<FM_Mode>Auto","<FM_Mode>Mono").replace("<Stereo>Negate","<Stereo>Assert"))
        assertEquals("FM",fm.band);assertEquals(FmMode.MONO,fm.fmMode);assertEquals(true,fm.stereo)
        assertNull(parser.tuner(info().replace("<FM_Mode>Auto","<FM_Mode>Unknown")).fmMode)
    }
    @Test fun amIgnoresStaleFmRdsAndMode() {
        val am=parser.tuner(info().replace("<Band>FM","<Band>AM")
            .replace("<Program_Service></Program_Service>","<Program_Service>Old FM</Program_Service>")
            .replace("<Current><Val>9740</Val><Exp>2</Exp><Unit>MHz</Unit></Current>",
                "<Current><Val>1134</Val><Exp>0</Exp><Unit>kHz</Unit></Current>"))
        assertEquals("AM",am.band);assertEquals("1134 kHz",am.frequency?.display)
        assertNull(am.fmMode);assertNull(am.nowPlaying.station);assertNull(am.clockTime)
    }
    @Test fun richRdsUsesSharedTrimmedMetadata() {
        val text=info().replace("<Program_Service></Program_Service>","<Program_Service> Station </Program_Service>")
            .replace("<Radio_Text_A></Radio_Text_A>","<Radio_Text_A>Programme</Radio_Text_A>")
            .replace("<Radio_Text_B></Radio_Text_B>","<Radio_Text_B>Presenter</Radio_Text_B>")
            .replace("<Program_Type></Program_Type>","<Program_Type>News</Program_Type>")
            .replace("<Clock_Time></Clock_Time>","<Clock_Time> 12:34 </Clock_Time>")
        val t=parser.tuner(text)
        assertEquals("Station",t.nowPlaying.station);assertEquals("Programme · Presenter",t.nowPlaying.title)
        assertEquals("News",t.programType);assertEquals("12:34",t.clockTime)
    }
    @Test fun duplicateAndMissingRdsAreOmitted() {
        var xml=info().replace("<Program_Service></Program_Service>","<Program_Service>Station</Program_Service>")
            .replace("<Radio_Text_A></Radio_Text_A>","<Radio_Text_A>station</Radio_Text_A>")
            .replace("<Radio_Text_B></Radio_Text_B>","<Radio_Text_B>Station</Radio_Text_B>")
        assertNull(parser.tuner(xml).nowPlaying.title)
        xml=info().replace("<Radio_Text_A></Radio_Text_A>","<Radio_Text_A>Programme</Radio_Text_A>")
            .replace("<Radio_Text_B></Radio_Text_B>","<Radio_Text_B>programme</Radio_Text_B>")
        assertEquals("Programme",parser.tuner(xml).nowPlaying.title)
        val missing=parser.tuner(info())
        assertNull(missing.nowPlaying.station);assertNull(missing.nowPlaying.title)
        assertNull(missing.programType);assertNull(missing.clockTime)
    }
    @Test fun firmwareIsOptionalOpaqueVersion() {
        assertEquals("1.13/0.05",parser.config("""<YAMAHA_AV rsp="GET" RC="0"><System><Config><Model_Name>R-N301</Model_Name><Version> 1.13/0.05 </Version></Config></System></YAMAHA_AV>""").firmware)
        assertNull(parser.config(configXml).firmware)
        assertNull(parser.config(configXml.replace("</Config>","<Version> </Version></Config>")).firmware)
    }
    @Test fun malformedResponsesRemainStructuredErrors() {
        assertThrows(YamahaException::class.java){parser.tuner("<YAMAHA_AV>")}
        assertThrows(YamahaException::class.java){parser.config("<YAMAHA_AV>")}
    }
}

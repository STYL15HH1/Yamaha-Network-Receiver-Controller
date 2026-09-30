package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.browser.*
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.settings.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class V075BrowserTest {
    private fun browser(fake:NetRadioFake, limit:Int=64, timeout:Long=90_000) =
        AggregatingMediaBrowser(NetRadioMediaBrowser(fake,YamahaXmlParser()),limit,timeout)
    private fun populated(count:Int)=NetRadioFake().apply {
        tree[""]=(1..count).map{"Station $it" to "Item"}
    }
    @Test fun fiveSavedFavoritesMigrateInOrderAndPersistFirstFour() {
        val store=MemorySettingsStore().apply{write("quick_sources","CD,OPTICAL,TUNER,Spotify,LINE1")}
        val expected=listOf("CD","OPTICAL","TUNER","Spotify")
        assertEquals(expected,AppSettings(store).state.value.quickSources)
        assertEquals("CD,OPTICAL,TUNER,Spotify",store.read("quick_sources"))
        assertEquals(expected,AppSettings(store).state.value.quickSources)
        assertEquals(4,QuickSources.MAX)
        assertEquals(expected,QuickSources.decode(QuickSources.encode(expected+"LINE1")))
    }
    @Test fun seventeenEntriesAggregateAndLastPageEntrySelectsOriginalLine()=runTest {
        val fake=populated(17);val browser=browser(fake)
        val all=browser.getCurrentList("receiver")
        assertEquals(17,all.items.size);assertTrue(all.aggregated);assertNull(all.page);assertNull(all.pages)
        assertEquals(listOf(1,2,3),all.items.map{it.originPage}.distinct())
        assertEquals(2,fake.commands.filterIsInstance<YamahaCommand.NetRadioPage>().size)
        fake.commands.clear()
        browser.selectItem("receiver",all,all.items[9])
        assertTrue(fake.commands.contains(YamahaCommand.NetRadioSelect(2)))
        assertTrue(fake.commands.contains(YamahaCommand.NetRadioPage(false)))
        assertTrue(fake.commands.contains(YamahaCommand.NetRadioControl(PlayerAction.PLAY)))
    }
    @Test fun singlePageDoesNotSendPageCommands()=runTest {
        val fake=populated(8);val browser=browser(fake)
        val all=browser.getCurrentList("receiver")
        browser.selectItem("receiver",all,all.items.first())
        assertTrue(fake.commands.none{it is YamahaCommand.NetRadioPage})
    }
    @Test fun emptyMenuIsCompleteWithoutPaging()=runTest {
        val fake=populated(0)
        assertTrue(browser(fake).getCurrentList("receiver").items.isEmpty())
        assertTrue(fake.commands.none{it is YamahaCommand.NetRadioPage})
    }
    @Test fun initialLaterPageIsRewoundAndOrderRestored()=runTest {
        val fake=populated(17).apply{page=3}
        val list=browser(fake).getCurrentList("receiver")
        assertEquals("Station 1",list.items.first().title);assertEquals("Station 17",list.items.last().title)
        assertEquals(4,fake.commands.filterIsInstance<YamahaCommand.NetRadioPage>().size)
    }
    @Test fun equalNamesAcrossPagesRemainDistinctAndSelectable()=runTest {
        val fake=populated(9).apply{tree[""]=List(9){"Same station" to "Item"}}
        val b=browser(fake);val list=b.getCurrentList("receiver")
        assertEquals(9,list.items.map{it.originPage to it.line}.distinct().size)
        fake.commands.clear();b.selectItem("receiver",list,list.items.last())
        assertEquals(2,fake.page);assertTrue(fake.commands.contains(YamahaCommand.NetRadioSelect(1)))
    }
    @Test fun staleTargetPageCannotSelectWrongEntry()=runTest {
        val fake=populated(9);val b=browser(fake);val list=b.getCurrentList("receiver")
        fake.tree[""]=List(9){"Changed" to "Item"};fake.commands.clear()
        try{b.selectItem("receiver",list,list.items.first());fail("Expected changed menu")}
        catch(e:BrowserException){assertEquals(BrowserFailure.CHANGED,e.failure)}
        assertTrue(fake.commands.none{it is YamahaCommand.NetRadioSelect})
    }
    @Test fun oversizedMenuFailsBeforeAnyPaging()=runTest {
        val fake=populated(17)
        try{browser(fake,2).getCurrentList("receiver");fail("Expected limit")}
        catch(e:BrowserException){assertEquals(BrowserFailure.LIMIT_REACHED,e.failure)}
        assertTrue(fake.commands.none{it is YamahaCommand.NetRadioPage})
    }
    @Test fun nonAdvancingPageTerminatesWithExistingReadinessError()=runTest {
        val fake=populated(9).apply{stuck=true}
        try{browser(fake).getCurrentList("receiver");fail("Expected termination")}
        catch(e:BrowserException){assertEquals(BrowserFailure.NOT_READY,e.failure)}
        assertEquals(1,fake.commands.filterIsInstance<YamahaCommand.NetRadioPage>().size)
    }
    @Test fun traversalTimeoutDoesNotReturnPartialMenu()=runTest {
        val fake=populated(17).apply{listDelay=1000}
        try{browser(fake,timeout=1500).getCurrentList("receiver");fail("Expected timeout")}
        catch(e:BrowserException){assertEquals(BrowserFailure.TIMEOUT,e.failure)}
    }
    @Test fun backwardAndSkippedPageTransitionsAreRejected()=runTest {
        val fake=populated(17).apply{wrongPage=true}
        try{browser(fake).getCurrentList("receiver");fail("Expected changed")}
        catch(e:BrowserException){assertEquals(BrowserFailure.CHANGED,e.failure)}
    }
    @Test fun directoriesBackHomeAndRefreshPreservePaths()=runTest {
        val fake=NetRadioFake();val b=browser(fake)
        var list=b.getCurrentList("receiver")
        list=b.selectItem("receiver",list,list.items.single())
        assertEquals(listOf("Locations"),b.navigationPath)
        list=b.selectItem("receiver",list,list.items.single())
        assertEquals(listOf("Locations","Europe"),b.navigationPath)
        assertEquals("Locations",b.goBack("receiver").title)
        assertTrue(b.goHome("receiver").root)
        assertTrue(b.getCurrentList("receiver").root)
    }
    @Test fun countryNamesAndAliasesResolveOffline() {
        for((name,code) in mapOf("Japan" to "JP","Mexico" to "MX","Myanmar" to "MM",
            "Peru" to "PE","Poland" to "PL","Portugal" to "PT","South Korea" to "KR","Polska" to "PL"))
            assertEquals(code,CountryFlags.code(name))
        assertEquals("🇯🇵",CountryFlags.flag(" Japan "))
        assertNull(CountryFlags.flag("Unmapped catalogue"))
    }
    @Test fun flagsRequireCountryMenuAndDirectoryNotStation() {
        val item=MediaItem(1,"Japan","Container","NET RADIO")
        val list=MediaList("NET RADIO","Ready",2,"Countries",1,1,listOf(item))
        assertEquals("🇯🇵",CountryFlags.forEntry(list,item))
        assertNull(CountryFlags.forEntry(list.copy(title="Genres"),item))
        assertNull(CountryFlags.forEntry(list,item.copy(attribute="Item")))
        assertNull(CountryFlags.forEntry(list,item.copy(title="Unknown")))
    }
}

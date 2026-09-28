package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.browser.*
import com.styl15hh1.rn301controller.data.protocol.*
import com.styl15hh1.rn301controller.data.model.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NetRadioNavigationTest {
    private fun browser(f:NetRadioFake)=NetRadioMediaBrowser(f,YamahaXmlParser())
    private suspend fun failure(expected:BrowserFailure,block:suspend ()->Unit) {
        try {block();fail("Expected $expected")}catch(e:BrowserException){assertEquals(expected,e.failure)}
    }
    @Test fun nestedDirectoriesTrackActualNamesAndBackRestoresParent()=runTest {
        val f=NetRadioFake();val b=browser(f)
        var list=b.getCurrentList("ip")
        assertEquals(emptyList<String>(),b.navigationPath)
        list=b.selectItem("ip",list,list.items.single())
        assertEquals(listOf("Locations"),b.navigationPath)
        list=b.selectItem("ip",list,list.items.single())
        assertEquals(listOf("Locations","Europe"),b.navigationPath)
        assertEquals(3,list.layer)
        b.goBack("ip");assertEquals(listOf("Locations"),b.navigationPath)
        b.goHome("ip");assertEquals(emptyList<String>(),b.navigationPath)
    }
    @Test fun stationSelectionProducesLogicalFavoriteAndReadsPlayback()=runTest {
        val f=NetRadioFake();val b=browser(f)
        RadioPathNavigator(b).navigatePath("ip",listOf("Locations","Europe","Radio"))
        assertEquals(RadioFavorite("Radio",listOf("Locations","Europe","Radio")),b.selectedStation)
        val select=f.commands.indexOfLast{it is YamahaCommand.NetRadioSelect}
        assertTrue(f.commands.indexOf(YamahaCommand.NetRadioControl(PlayerAction.PLAY))>select)
        assertTrue(f.commands.contains(YamahaCommand.NetRadioInfo))
    }
    @Test fun pathSearchFindsFirstSecondAndLaterPages()=runTest {
        for(target in listOf(1,9,23)) {
            val f=NetRadioFake()
            f.tree[""]=(1..25).map{"Station $it" to "Item"}
            val b=browser(f)
            RadioPathNavigator(b).navigatePath("ip",listOf("Station $target"))
            assertEquals("Station $target",b.selectedStation!!.displayName)
            assertEquals(YamahaCommand.NetRadioSelect((target-1)%8+1),f.commands.filterIsInstance<YamahaCommand.NetRadioSelect>().single())
        }
    }
    @Test fun missingExactComponentNeverSelectsSimilarStation()=runTest {
        val f=NetRadioFake();f.tree[""]=listOf("Radio 1" to "Item")
        failure(BrowserFailure.PATH_UNAVAILABLE){RadioPathNavigator(browser(f)).navigatePath("ip",listOf("Radio"))}
        assertTrue(f.commands.none{it is YamahaCommand.NetRadioSelect})
    }
    @Test fun unicodeAndXmlEntitiesSurvivePathReplay()=runTest {
        val f=NetRadioFake();f.tree[""]=listOf("日本 & Polska" to "Item")
        val b=browser(f);RadioPathNavigator(b).navigatePath("ip",listOf("日本 & Polska"))
        assertEquals(listOf("日本 & Polska"),b.selectedStation!!.navigationPath)
    }
    @Test fun duplicateNamesAcrossPagesAreRejectedWithoutSelecting()=runTest {
        val f=NetRadioFake();f.tree[""]=(1..9).map{(if(it==1 || it==9) "Same" else "Other $it") to "Item"}
        failure(BrowserFailure.PATH_AMBIGUOUS){RadioPathNavigator(browser(f)).navigatePath("ip",listOf("Same"))}
        assertTrue(f.commands.none{it is YamahaCommand.NetRadioSelect})
    }
    @Test fun exactPathDoesNotTreatDirectoryAsStation()=runTest {
        val f=NetRadioFake()
        failure(BrowserFailure.PATH_UNAVAILABLE){RadioPathNavigator(browser(f)).navigatePath("ip",listOf("Locations"))}
        assertTrue(f.commands.none{it is YamahaCommand.NetRadioSelect})
    }
    @Test fun slowBusyToReadyIsBoundedAndDoesNotResendSelection()=runTest {
        val f=NetRadioFake().apply{busyReads=3;busyAfterSelection=4}
        val b=browser(f);val list=b.getCurrentList("ip")
        b.selectItem("ip",list,list.items.single())
        assertTrue(testScheduler.currentTime>=7700)
        assertEquals(1,f.commands.filterIsInstance<YamahaCommand.NetRadioSelect>().size)
    }
    @Test fun loadingTransitionsToReadyAndHomeHonorsExactReturnLimit()=runTest {
        val f=NetRadioFake().apply{busyReads=2;busyStatus="Loading"}
        val b=browser(f);assertTrue(b.getCurrentList("ip").ready)
        f.path+=(1..32).map{"Level $it"}
        assertTrue(b.goHome("ip").root)
        assertEquals(32,f.commands.count{it==YamahaCommand.NetRadioBack})
    }
    @Test fun homeStopsWhenReceiverIgnoresReturn()=runTest {
        val f=NetRadioFake().apply{path+="Locations";stuck=true}
        failure(BrowserFailure.NOT_READY){browser(f).goHome("ip")}
        assertEquals(1,f.commands.count{it==YamahaCommand.NetRadioBack})
    }
    @Test fun sourceGuardRejectsPathBeforeSelectingStation()=runTest {
        val f=NetRadioFake();f.tree[""]=listOf("Radio" to "Item")
        var calls=0
        failure(BrowserFailure.INACTIVE){
            RadioPathNavigator(browser(f)).navigatePath("ip",listOf("Radio")){
                if(++calls>1)throw BrowserException(BrowserFailure.INACTIVE)
            }
        }
        assertTrue(f.commands.none{it is YamahaCommand.NetRadioSelect})
    }
    @Test fun loadingStatusIsToleratedButEventuallyStops()=runTest {
        val f=NetRadioFake().apply{menuStatus="Loading"}
        failure(BrowserFailure.NOT_READY){browser(f).getCurrentList("ip")}
        assertEquals(20,f.commands.count{it==YamahaCommand.NetRadioList})
    }
    @Test fun serviceStatusDoesNotBecomeReceiverNotFound()=runTest {
        val f=NetRadioFake().apply{menuStatus="Unavailable"}
        failure(BrowserFailure.SERVICE_UNAVAILABLE){browser(f).getCurrentList("ip")}
    }
    @Test fun changedMenuPreventsStaleDirectSelection()=runTest {
        val f=NetRadioFake();val b=browser(f);val list=b.getCurrentList("ip")
        f.tree[""]=listOf("Something else" to "Container")
        failure(BrowserFailure.CHANGED){b.selectItem("ip",list,list.items.single())}
        assertTrue(f.commands.none{it is YamahaCommand.NetRadioSelect})
    }
    @Test fun unchangedReadyMenuDoesNotPretendDirectoryTransitionSucceeded()=runTest {
        val f=NetRadioFake().apply{stuck=true};val b=browser(f);val list=b.getCurrentList("ip")
        failure(BrowserFailure.NOT_READY){b.selectItem("ip",list,list.items.single())}
        assertEquals(1,f.commands.filterIsInstance<YamahaCommand.NetRadioSelect>().size)
    }
    @Test fun inconsistentPageAndPageBudgetTerminate()=runTest {
        val f=NetRadioFake();f.tree[""]=(1..25).map{"Station $it" to "Item"}
        failure(BrowserFailure.LIMIT_REACHED){RadioPathNavigator(browser(f),maxPages=2).navigatePath("ip",listOf("Station 25"))}
        f.page=1;f.wrongPage=true
        failure(BrowserFailure.CHANGED){RadioPathNavigator(browser(f)).navigatePath("ip",listOf("Station 25"))}
    }
    @Test fun depthAndTotalTimeAreBounded()=runTest {
        val f=NetRadioFake()
        failure(BrowserFailure.PATH_UNAVAILABLE){RadioPathNavigator(browser(f),maxDepth=1).navigatePath("ip",listOf("A","B"))}
        f.listDelay=2000
        failure(BrowserFailure.TIMEOUT){RadioPathNavigator(browser(f),timeoutMs=1000).navigatePath("ip",listOf("Radio"))}
    }
    @Test fun failedPlaybackDoesNotCreateFavorite()=runTest {
        val f=NetRadioFake().apply{playback="Stop"};f.tree[""]=listOf("Radio" to "Item")
        val b=browser(f)
        failure(BrowserFailure.PLAYBACK_FAILED){RadioPathNavigator(b).navigatePath("ip",listOf("Radio"))}
        assertNull(b.selectedStation)
    }
    @Test fun externalNavigationInvalidatesUnknownPathInsteadOfInventingAncestors()=runTest {
        val f=NetRadioFake();f.path+=listOf("Locations","Europe")
        val b=browser(f);b.getCurrentList("ip");assertNull(b.navigationPath)
        b.goHome("ip");assertEquals(emptyList<String>(),b.navigationPath)
    }
}

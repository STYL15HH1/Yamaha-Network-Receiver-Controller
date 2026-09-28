package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.Source
import com.styl15hh1.rn301controller.data.settings.*
import org.junit.Assert.*
import org.junit.Test

class FavoritesTest {
    @Test fun cleanInstallDefaultsUseReceiverAvailableSourcesInPreferredOrder() {
        val s = AppSettings(MemorySettingsStore()).state.value
        assertEquals(listOf("OPTICAL", "TUNER", "Spotify"), QuickSources.ids(s, Source.known))
        assertEquals(listOf("TUNER"), QuickSources.ids(s, listOf(Source("TUNER"), Source("OPTICAL", selectable=false))))
        assertTrue(QuickSources.ids(s, emptyList()).isEmpty())
    }
    @Test fun existingQuickSourcesKeyAndOrderAreReusedWithoutRewrite() {
        val store = MemorySettingsStore()
        val encoded = QuickSources.encode(listOf("LINE2", "CD", "Spotify"))
        store.write("quick_sources", encoded)
        val state = AppSettings(store).state.value
        assertTrue(state.quickSourcesConfigured)
        assertEquals(listOf("LINE2", "CD", "Spotify"), QuickSources.ids(state, Source.known))
        assertEquals(encoded, store.read("quick_sources"))
    }
    @Test fun explicitlyEmptyPreferenceNeverBecomesDefaults() {
        val store = MemorySettingsStore()
        store.write("quick_sources", "")
        assertTrue(QuickSources.ids(AppSettings(store).state.value, Source.known).isEmpty())
    }
    @Test fun customizationPersistsIncludingRemovingEveryFavorite() {
        val store = MemorySettingsStore(); val settings = AppSettings(store)
        settings.quickSources(listOf("CD", "OPTICAL"))
        assertEquals(listOf("CD", "OPTICAL"), QuickSources.ids(AppSettings(store).state.value, Source.known))
        settings.quickSources(emptyList())
        assertTrue(QuickSources.ids(AppSettings(store).state.value, Source.known).isEmpty())
    }
    @Test fun unavailableFavoriteHiddenWithoutDestroyingSavedConfiguration() {
        val settings = AppSettings(MemorySettingsStore())
        settings.quickSources(listOf("CD", "OPTICAL", "Removed"))
        val ids = QuickSources.ids(settings.state.value, Source.known)
        assertEquals(listOf("CD"), QuickSources.available(ids, listOf(Source("CD"))).map { it.id })
        assertEquals(listOf("CD", "OPTICAL", "Removed"), settings.state.value.quickSources)
    }
}

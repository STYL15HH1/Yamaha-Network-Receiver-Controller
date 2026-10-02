package com.styl15hh1.rn301controller

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.widget.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
class WidgetIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager get() = shadowOf(AppWidgetManager.getInstance(context))
    private val status = ReceiverStatus(connectionState = ConnectionState.CONNECTED, address = "receiver", modelName = "R-N301",
        powerState = PowerState.ON, currentSource = Source("TUNER"), sources = listOf(Source("TUNER"), Source("SERVER")))
    private val tuner = TunerState(status = TunerStatus(nowPlaying = NowPlaying("TUNER", station = "TOK FM")))
    @Before fun reset() {
        listOf("widget_snapshot", "application_settings", "receiver").forEach { context.getSharedPreferences(it, 0).edit().clear().commit() }
        context.getSharedPreferences("receiver", 0).edit().putString("address", "receiver").commit()
    }
    private fun snapshot(state: ReceiverStatus = status, settings: SettingsState = SettingsState()) = WidgetSnapshot(state, tuner, PlayerState(), settings)
    private fun text(id: Int, view: Int) = manager.getViewFor(id).findViewById<TextView>(view).text.toString()
    @Test fun allInstancesRenderSameReceiverAndMetadata() {
        val first = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val second = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        ReceiverWidget.capture(context, snapshot())
        for (id in listOf(first, second)) {
            assertEquals("R-N301", text(id, R.id.widget_receiver))
            assertEquals("TOK FM", text(id, R.id.widget_metadata))
            assertEquals("Power, On", manager.getViewFor(id).findViewById<View>(R.id.widget_power).contentDescription)
            assertEquals(View.GONE, manager.getViewFor(id).findViewById<View>(R.id.widget_status).visibility)
        }
    }
    @Test fun receiverFailureKeepsLastSourceButMarksUnavailable() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        ReceiverWidget.capture(context, snapshot())
        ReceiverWidget.capture(context, snapshot(status.copy(connectionState = ConnectionState.UNAVAILABLE,
            stale = true, powerState = PowerState.UNAVAILABLE, currentSource = null)))
        assertEquals("Tuner", text(id, R.id.widget_source))
        assertTrue(text(id, R.id.widget_status).startsWith("Last updated "))
        assertEquals(View.VISIBLE, manager.getViewFor(id).findViewById<View>(R.id.widget_status).visibility)
        assertEquals("TOK FM", text(id, R.id.widget_metadata))
    }
    @Test fun favoritesChangesAreAppliedToEveryInstanceIncludingEmptyChoice() {
        val first = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val second = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val settings = AppSettings(PreferencesSettingsStore(context))
        settings.quickSources(listOf("SERVER", "TUNER"))
        ReceiverWidget.capture(context, snapshot(settings = settings.state.value))
        assertEquals("Server", text(first, R.id.widget_favorite_1))
        assertEquals("Tuner", text(second, R.id.widget_favorite_2))
        settings.quickSources(emptyList())
        ReceiverWidget.capture(context, snapshot(settings = settings.state.value))
        for (id in listOf(first, second)) assertEquals(View.GONE, manager.getViewFor(id).findViewById<View>(R.id.widget_favorite_1).visibility)
    }
    @Test fun cacheSurvivesProviderRecreationButDoesNotLeakToDifferentSavedReceiver() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        ReceiverWidget.capture(context, snapshot())
        ReceiverWidget().onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(id))
        assertEquals("TOK FM", text(id, R.id.widget_metadata))
        context.getSharedPreferences("receiver", 0).edit().putString("address", "different").commit()
        ReceiverWidget.updateAll(context)
        assertEquals("Connect your receiver", text(id, R.id.widget_source))
    }
    @Test fun pendingIntentsAreImmutableAndUniquePerInstanceAndSource() {
        val first = ReceiverWidget.action(context, 1, WidgetAction.SOURCE, "SERVER")
        val second = ReceiverWidget.action(context, 2, WidgetAction.SOURCE, "SERVER")
        val other = ReceiverWidget.action(context, 1, WidgetAction.SOURCE, "TUNER")
        assertNotEquals(first, second); assertNotEquals(first, other)
        assertEquals("SERVER", shadowOf(first).savedIntent.getStringExtra("source"))
        assertEquals(1, shadowOf(first).savedIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
        assertTrue(first.isImmutable)
    }
    @Test fun zeroThroughFourFavoritesKeepCompactSlotsAndNoEmptyRow() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val choices = listOf("OPTICAL", "TUNER", "Spotify", "NET RADIO")
        val settings = AppSettings(PreferencesSettingsStore(context))
        val tiles = listOf(R.id.widget_tile_1, R.id.widget_tile_2, R.id.widget_tile_3, R.id.widget_tile_4)
        for (count in 0..4) {
            settings.quickSources(choices.take(count))
            ReceiverWidget.capture(context, snapshot(status.copy(sources = choices.map { Source(it) }), settings.state.value))
            val root = manager.getViewFor(id)
            assertEquals(count, tiles.count { root.findViewById<View>(it).visibility == View.VISIBLE })
            assertEquals(if (count == 0) View.GONE else View.VISIBLE, root.findViewById<View>(R.id.widget_favorites).visibility)
        }
    }
    @Test fun currentSourceChangesSelectedTileAndAccessibleLabel() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val settings = AppSettings(PreferencesSettingsStore(context)).apply { quickSources(listOf("TUNER", "SERVER")) }
        ReceiverWidget.capture(context, snapshot(settings = settings.state.value))
        assertNotEquals(manager.getViewFor(id).findViewById<TextView>(R.id.widget_favorite_1).currentTextColor,
            manager.getViewFor(id).findViewById<TextView>(R.id.widget_favorite_2).currentTextColor)
        assertEquals("Tuner, Selected", manager.getViewFor(id).findViewById<View>(R.id.widget_tile_1).contentDescription)
        ReceiverWidget.capture(context, snapshot(status.copy(currentSource = Source("SERVER")), settings.state.value))
        assertEquals("Tuner", manager.getViewFor(id).findViewById<View>(R.id.widget_tile_1).contentDescription)
        assertEquals("Server, Selected", manager.getViewFor(id).findViewById<View>(R.id.widget_tile_2).contentDescription)
    }
    @Test fun staleShowsTimeOnlyAndRecoveryHidesStatusAgain() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        ReceiverWidget.capture(context, snapshot())
        ReceiverWidget.capture(context, snapshot(status.copy(stale = true)))
        assertEquals(View.VISIBLE, manager.getViewFor(id).findViewById<View>(R.id.widget_status).visibility)
        val message = text(id, R.id.widget_status)
        assertTrue(message.startsWith("Last updated "))
        assertFalse(message.contains(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()))
        ReceiverWidget.capture(context, snapshot())
        assertEquals(View.GONE, manager.getViewFor(id).findViewById<View>(R.id.widget_status).visibility)
    }
    @Test fun unavailableWithoutCacheAndMissingMetadataRemainReadable() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        assertEquals("Receiver unavailable", text(id, R.id.widget_status))
        ReceiverWidget.capture(context, WidgetSnapshot(status, TunerState(), PlayerState(), SettingsState()))
        assertEquals(View.GONE, manager.getViewFor(id).findViewById<View>(R.id.widget_metadata).visibility)
        assertEquals("Tuner", text(id, R.id.widget_source))
    }
    @Test fun lightAndDarkUseApplicationAccentForSelectedSource() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val settings = AppSettings(PreferencesSettingsStore(context)).apply { quickSources(listOf("TUNER", "SERVER")) }
        for ((appearance, accent, foreground) in listOf(
            Triple(Appearance.LIGHT, 0xFF146B55.toInt(), 0xFF18211D.toInt()),
            Triple(Appearance.DARK, 0xFF89DEC6.toInt(), 0xFFE6EDEC.toInt()))) {
            settings.appearance(appearance)
            ReceiverWidget.capture(context, snapshot(settings = settings.state.value))
            val root = manager.getViewFor(id)
            assertEquals(accent, root.findViewById<TextView>(R.id.widget_favorite_1).currentTextColor)
            assertEquals(foreground, root.findViewById<TextView>(R.id.widget_favorite_2).currentTextColor)
        }
    }

    @Test fun powerIconShowsOnStandbyAndOfflineSemantics() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        for ((state, label, alpha) in listOf(
            Triple(status, "Power, On", 255),
            Triple(status.copy(powerState = PowerState.STANDBY), "Power, Standby", 255),
            Triple(status.copy(stale = true), "Power, Receiver unavailable", 128))) {
            ReceiverWidget.capture(context, snapshot(state))
            val button = manager.getViewFor(id).findViewById<android.widget.ImageButton>(R.id.widget_power)
            assertEquals(label, button.contentDescription)
            assertEquals(alpha, button.imageAlpha)
        }
    }

    @Test fun sourceCardOpensTheExistingMainActivity() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        ReceiverWidget.capture(context, snapshot())
        assertTrue(manager.getViewFor(id).findViewById<View>(R.id.widget_info).performClick())
        assertEquals(MainActivity::class.java.name, shadowOf(context as android.app.Application).nextStartedActivity.component?.className)
    }

    @Test fun richTunerDetailsSurviveOfflineAndClearOnSourceChange() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val rich = TunerState(status = TunerStatus(frequency = ReceiverFrequency(9740, 2, "MHz"), preset = 1,
            stereo = true, nowPlaying = NowPlaying("TUNER", station = "TOK FM")))
        ReceiverWidget.capture(context, WidgetSnapshot(status, rich, PlayerState(), SettingsState()))
        assertEquals("97.4 MHz · Preset 1 · Stereo", text(id, R.id.widget_secondary))
        ReceiverWidget.capture(context, WidgetSnapshot(status.copy(stale = true, currentSource = null), TunerState(), PlayerState(), SettingsState()))
        assertTrue(manager.getViewFor(id).findViewById<View>(R.id.widget_info).contentDescription.toString().contains("97.4 MHz · Preset 1 · Stereo"))
        ReceiverWidget.capture(context, snapshot(status.copy(currentSource = Source("OPTICAL"))))
        assertEquals(View.GONE, manager.getViewFor(id).findViewById<View>(R.id.widget_secondary).visibility)
        assertEquals("", text(id, R.id.widget_metadata))
    }

    @Test fun tileClickStillDispatchesTheExistingWidgetJob() {
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        val settings = AppSettings(PreferencesSettingsStore(context)).apply { quickSources(listOf("SERVER")) }
        ReceiverWidget.capture(context, snapshot(settings = settings.state.value))
        val scheduler = context.getSystemService(android.app.job.JobScheduler::class.java)
        scheduler.cancelAll()
        assertTrue(manager.getViewFor(id).findViewById<View>(R.id.widget_tile_1).performClick())
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(WidgetJobService::class.java.name, scheduler.allPendingJobs.single().service.className)
    }

}

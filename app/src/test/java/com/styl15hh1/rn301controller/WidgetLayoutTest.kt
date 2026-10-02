package com.styl15hh1.rn301controller

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Render the actual RemoteViews, including platform text measurement and drawable assets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetLayoutTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun render(appearance: Appearance, width: Int, height: Int, offline: Boolean, favoriteCount: Int = 4, rich: Boolean = false): View {
        context.getSharedPreferences("receiver", 0).edit().putString("address", "receiver").commit()
        val settings = AppSettings(PreferencesSettingsStore(context)).apply {
            appearance(appearance); quickSources(listOf("OPTICAL", "TUNER", "Spotify", "NET RADIO").take(favoriteCount))
        }
        val manager = shadowOf(AppWidgetManager.getInstance(context))
        val id = manager.createWidget(ReceiverWidget::class.java, R.layout.receiver_widget)
        AppWidgetManager.getInstance(context).updateAppWidgetOptions(id, android.os.Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height) })
        val status = ReceiverStatus(connectionState = ConnectionState.CONNECTED, address = "receiver", modelName = "R-N301",
            powerState = PowerState.ON, currentSource = Source("TUNER"))
        val tuner = TunerState(status = TunerStatus(frequency = if (rich) ReceiverFrequency(9740, 2, "MHz") else null, preset = if (rich) 1 else null, stereo = if (rich) true else null, nowPlaying = NowPlaying("TUNER", station = "TOK FM")))
        ReceiverWidget.capture(context, WidgetSnapshot(status, tuner, PlayerState(), settings.state.value))
        if (offline) ReceiverWidget.capture(context, WidgetSnapshot(status.copy(stale = true), tuner, PlayerState(), settings.state.value))
        val view = manager.getViewFor(id)
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        return view
    }
    @Test fun compactOnlineAndOfflineLayoutsFitAndKeepTilesAdjacent() {
        for (offline in listOf(false, true)) {
            val root = render(Appearance.DARK, 250, 132, offline)
            val header = root.findViewById<View>(R.id.widget_header)
            val favorites = root.findViewById<View>(R.id.widget_favorites)
            assertTrue("Header clipped at $offline", header.top >= root.paddingTop)
            assertEquals(4, favorites.top - header.bottom)
            assertTrue("Favorites clipped at $offline", favorites.bottom <= root.height - root.paddingBottom)
            val status = root.findViewById<TextView>(R.id.widget_status)
            assertEquals(if (offline) View.VISIBLE else View.GONE, status.visibility)
        }
    }
    @Test fun largerHostsDoNotStretchTheGapAndRenderBothAppearances() {
        val directory = File("build/outputs/widget-previews").apply { mkdirs() }
        for (appearance in listOf(Appearance.LIGHT, Appearance.DARK)) {
            for ((width, height) in listOf(250 to 132, 340 to 180, 340 to 220)) {
                val root = render(appearance, width, height, false)
                val favorites = root.findViewById<View>(R.id.widget_favorites)
                val card = root.findViewById<View>(R.id.widget_info)
                if (height >= 180) {
                    assertEquals(4, card.top - root.findViewById<View>(R.id.widget_header).bottom)
                    assertEquals(4, favorites.top - card.bottom)
                    assertTrue(card.height >= 56)
                }
                assertEquals(height - root.paddingBottom, favorites.bottom)
                assertEquals(48, root.findViewById<View>(R.id.widget_power).height)
                assertEquals(44, root.findViewById<View>(R.id.widget_refresh).height)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                File(directory, "widget-${appearance.name.lowercase()}-$height.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
    @Test fun oneFavoriteIsCenteredWithoutStretching() = assertCenteredFavorites(1)
    @Test fun twoFavoritesAreCenteredWithoutStretching() = assertCenteredFavorites(2)
    @Test fun threeFavoritesAreCenteredWithoutStretching() = assertCenteredFavorites(3)
    @Test fun fourFavoritesFillTheRow() = assertCenteredFavorites(4)

    private fun assertCenteredFavorites(count: Int) {
        val ids = listOf(R.id.widget_tile_1, R.id.widget_tile_2, R.id.widget_tile_3, R.id.widget_tile_4)
        for ((width, height) in listOf(250 to 132, 340 to 220)) {
            val root = render(Appearance.DARK, width, height, false, count)
            val row = root.findViewById<View>(R.id.widget_favorites)
            val tiles = ids.take(count).map { root.findViewById<View>(it) }
            assertTrue("Group must be horizontally centered", kotlin.math.abs(tiles.first().left - (row.width - tiles.last().right)) <= 1)
            tiles.forEach {
                // Fewer inter-tile margins change width by at most 3dp, never stretch to fill.
                assertTrue(kotlin.math.abs(it.width - ((row.width - 16) / 4)) <= 3)
                assertEquals(56, it.height)
            }
            tiles.zipWithNext().forEach { (left, right) -> assertEquals(4, right.left - left.right) }
            ids.drop(count).forEach { assertEquals(View.GONE, root.findViewById<View>(it).visibility) }
            if (count == 4) assertEquals(2, tiles.first().left)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            val directory = File("build/outputs/widget-previews").apply { mkdirs() }
            File(directory, "favorites-$count-$width.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun richSourceLinesFitWithoutChangingControlsOrFavorites() {
        for ((width, height) in listOf(250 to 132, 340 to 180, 340 to 220)) for (offline in listOf(false, true)) {
            val root = render(Appearance.DARK, width, height, offline, rich = true)
            val card = root.findViewById<View>(R.id.widget_info)
            val source = root.findViewById<View>(R.id.widget_source)
            val parent = source.parent as View
            assertTrue("Content clipped at $height offline=$offline", parent.height <= card.height)
            val secondary = root.findViewById<View>(R.id.widget_secondary)
            assertEquals(if (offline && height < 196) View.GONE else View.VISIBLE, secondary.visibility)
            assertEquals(48, root.findViewById<View>(R.id.widget_power).height)
            assertEquals(44, root.findViewById<View>(R.id.widget_refresh).height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File("build/outputs/widget-previews").mkdirs()
            File("build/outputs/widget-previews/rich-tuner-$height-$offline.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun bothProviderVersionsPermitCompactVerticalResize() {
        for (folder in listOf("xml", "xml-v31")) {
            val text = File("src/main/res/$folder/receiver_widget_info.xml").readText()
            assertTrue(text.contains("minHeight=\"132dp\""))
            assertTrue(text.contains("minResizeHeight=\"132dp\""))
            assertTrue(text.contains("resizeMode=\"horizontal|vertical\""))
            assertTrue(text.contains("updatePeriodMillis=\"0\""))
        }
        val modern = File("src/main/res/xml-v31/receiver_widget_info.xml").readText()
        assertTrue(modern.contains("targetCellWidth=\"4\"")); assertTrue(modern.contains("targetCellHeight=\"2\""))
    }
}

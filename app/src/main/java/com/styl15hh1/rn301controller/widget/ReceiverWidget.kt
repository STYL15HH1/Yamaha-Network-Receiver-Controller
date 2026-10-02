package com.styl15hh1.rn301controller.widget

import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt
import androidx.core.content.edit
import com.styl15hh1.rn301controller.MainActivity
import com.styl15hh1.rn301controller.R
import com.styl15hh1.rn301controller.ui.sourceDrawable
import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.settings.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

data class WidgetSnapshot(val status: ReceiverStatus, val tuner: TunerState, val player: PlayerState, val settings: SettingsState)

class ReceiverWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateAll(context)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (WidgetAction.entries.any { it.name == intent.action }) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (id in ids(context)) WidgetJobService.enqueue(context, intent)
        }
    }
    companion object {
        private fun ids(context: Context) = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ReceiverWidget::class.java))
        private fun preferences(context: Context) = context.getSharedPreferences("widget_snapshot", Context.MODE_PRIVATE)
        private fun read(context: Context): JSONObject = try { JSONObject(preferences(context).getString("snapshot", "{}") ?: "{}") }
            catch (_: org.json.JSONException) { JSONObject() }
        fun capture(context: Context, snapshot: WidgetSnapshot) {
            val state = snapshot.status
            if (state.address.isBlank()) { if (ids(context).isNotEmpty()) updateAll(context); return }
            val old = read(context).takeIf { it.optString("address") == state.address } ?: JSONObject()
            val model = WidgetPresentation.from(state, snapshot.tuner, snapshot.player, snapshot.settings)
            val sameSource = model.source == null || model.source.id == old.optString("source")
            val cachedDetails = if (model.offline && sameSource) old.optJSONObject("details") else null
            val details = model.tunerDetails?.let {
                JSONObject().put("frequency", it.frequency).put("preset", it.preset).put("reception", it.reception?.name)
            } ?: cachedDetails
            val json = JSONObject().put("address", state.address)
                .put("receiver", model.receiver ?: old.optString("receiver"))
                .put("power", if (model.offline) old.optString("power", model.power.name) else model.power.name)
                .put("source", model.source?.id ?: if (model.offline) old.optString("source") else "")
                .put("metadata", model.metadata ?: if (model.offline && sameSource) old.optString("metadata") else "")
                .put("secondary", model.secondary ?: if (model.offline && sameSource) old.optString("secondary") else "")
                .put("details", details)
                .put("offline", model.offline)
                .put("sources", if (model.offline && old.has("sources")) old.getJSONArray("sources") else JSONArray(state.sources.filter { it.selectable }.map { JSONObject().put("id", it.id).put("title", it.title) }))
            // Do not rewrite preferences or wake launcher for unrelated volume / tuner changes.
            val comparable = JSONObject(old.toString()).apply { remove("time") }
            if (json.toString() != comparable.toString()) {
                json.put("time", if (model.offline) old.optLong("time") else System.currentTimeMillis())
                preferences(context).edit { putString("snapshot", json.toString()) }
                updateAll(context)
            } else if (ids(context).isNotEmpty()) {
                // Favorites/preferences can change without changing receiver state.
                val encoded = snapshot.settings.language + snapshot.settings.appearance.name + QuickSources.encode(QuickSources.ids(snapshot.settings, state.sources))
                if (preferences(context).getString("favorites_revision", null) != encoded) {
                    preferences(context).edit { putString("favorites_revision", encoded) }
                    updateAll(context)
                }
            }
        }
        fun updateAll(base: Context) {
            val context = AndroidLocales.context(base)
            val saved = base.getSharedPreferences("receiver", Context.MODE_PRIVATE).getString("address", "")
            val json = read(base).takeIf { it.optString("address") == saved } ?: JSONObject()
            val settings = AppSettings(PreferencesSettingsStore(base)).state.value
            val sources = json.optJSONArray("sources")?.let { array -> (0 until array.length()).map { array.getJSONObject(it).let { entry -> Source(entry.getString("id"), entry.getString("title")) } } }.orEmpty()
            val favorites = QuickSources.available(QuickSources.ids(settings, sources), sources).take(4)
            val power = PowerState.entries.firstOrNull { it.name == json.optString("power") } ?: PowerState.UNAVAILABLE
            val manager = AppWidgetManager.getInstance(base)
            ids(base).forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                // Launcher options give portrait height as max and landscape height as min.
                val portrait = context.resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val height = options.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                    else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180))
                val views = RemoteViews(base.packageName, if (height < 180) R.layout.receiver_widget_compact else R.layout.receiver_widget)
                val dark = settings.appearance == Appearance.DARK || (settings.appearance == Appearance.SYSTEM &&
                    context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                views.setInt(R.id.widget_root, "setBackgroundResource", if (dark) R.drawable.widget_background else R.drawable.widget_background_light)
                val foreground = (if (dark) "#E6EDEC" else "#18211D").toColorInt()
                val accent = (if (dark) "#89DEC6" else "#146B55").toColorInt()
                listOf(R.id.widget_receiver, R.id.widget_metadata, R.id.widget_secondary, R.id.widget_status).forEach { views.setTextColor(it, foreground) }
                listOf(R.id.widget_source, R.id.widget_favorite_1, R.id.widget_favorite_2,
                    R.id.widget_favorite_3, R.id.widget_favorite_4).forEach { views.setTextColor(it, accent) }
                views.setInt(R.id.widget_refresh, "setColorFilter", accent)
                val open = PendingIntent.getActivity(base, id, Intent(base, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                views.setOnClickPendingIntent(R.id.widget_info, open)
                views.setTextViewText(R.id.widget_receiver, json.optString("receiver").ifBlank { context.getString(R.string.app_name) })
                val offline = json.optBoolean("offline", true)
                val active = power == PowerState.ON && !offline
                val neutralBackground = if (dark) R.drawable.widget_tile_dark else R.drawable.widget_tile_light
                views.setInt(R.id.widget_info, "setBackgroundResource", neutralBackground)
                views.setInt(R.id.widget_refresh, "setBackgroundResource", neutralBackground)
                views.setInt(R.id.widget_power, "setBackgroundResource", if (active) {
                    if (dark) R.drawable.widget_tile_dark_selected else R.drawable.widget_tile_light_selected
                } else neutralBackground)
                views.setInt(R.id.widget_power, "setColorFilter", if (active) accent else foreground)
                views.setInt(R.id.widget_power, "setImageAlpha", if (offline) 128 else 255)
                views.setContentDescription(R.id.widget_power, context.getString(R.string.power) + ", " + context.getString(when {
                    offline -> R.string.receiver_unavailable
                    power == PowerState.ON -> R.string.on
                    power == PowerState.STANDBY -> R.string.standby
                    else -> R.string.receiver_unavailable
                }))
                views.setImageViewResource(R.id.widget_source_icon, sourceDrawable(json.optString("source")))
                views.setInt(R.id.widget_source_icon, "setColorFilter", accent)
                views.setOnClickPendingIntent(R.id.widget_receiver, open)
                views.setOnClickPendingIntent(R.id.widget_power, action(base, id, WidgetAction.POWER))
                views.setTextViewText(R.id.widget_source, json.optString("source").takeIf { it.isNotBlank() }?.let { sourceLabel(context, it) }
                    ?: context.getString(R.string.connect_receiver))
                val metadata = json.optString("metadata").takeIf { power == PowerState.ON }.orEmpty()
                val detailJson = json.optJSONObject("details")
                val tunerDetail = detailJson?.let {
                    WidgetTunerDetails(it.optString("frequency").takeIf(String::isNotBlank),
                        it.optInt("preset").takeIf { number -> number > 0 },
                        WidgetReception.entries.firstOrNull { reception -> reception.name == it.optString("reception") })
                }?.text({ context.getString(R.string.preset_number, it) }, {
                    context.getString(when (it) {
                        WidgetReception.STEREO -> R.string.widget_stereo
                        WidgetReception.MONO -> R.string.reception_mono
                        WidgetReception.AUTO -> R.string.reception_auto
                    })
                })
                val secondary = (if (json.optString("source") == "TUNER") tunerDetail else json.optString("secondary"))
                    .takeIf { power == PowerState.ON }.orEmpty()
                // At the resize floor, stale status has priority over an extra line; retain all details in accessibility/cache.
                val mergeLines = offline && height < 196
                views.setTextViewText(R.id.widget_metadata, if (mergeLines) listOf(metadata, secondary).filter(String::isNotBlank).joinToString(" · ") else metadata)
                views.setTextViewText(R.id.widget_secondary, secondary)
                views.setViewVisibility(R.id.widget_secondary, if (secondary.isBlank() || mergeLines) View.GONE else View.VISIBLE)
                views.setContentDescription(R.id.widget_info, listOf(sourceLabel(context, json.optString("source")), metadata, secondary)
                    .filter(String::isNotBlank).joinToString(", "))
                views.setViewVisibility(R.id.widget_metadata, if (metadata.isBlank() && (!mergeLines || secondary.isBlank())) View.GONE else View.VISIBLE)
                val time = json.optLong("time")
                views.setViewVisibility(R.id.widget_status, if (offline) View.VISIBLE else View.GONE)
                views.setTextViewText(R.id.widget_status, if (time > 0)
                    context.getString(R.string.widget_last_updated, DateFormat.getTimeInstance(DateFormat.SHORT,
                        context.resources.configuration.locales[0]).format(Date(time)))
                    else context.getString(R.string.receiver_unavailable))
                views.setContentDescription(R.id.widget_refresh, context.getString(R.string.widget_refresh))
                views.setOnClickPendingIntent(R.id.widget_refresh, action(base, id, WidgetAction.REFRESH))
                val slots = listOf(R.id.widget_favorite_1, R.id.widget_favorite_2, R.id.widget_favorite_3, R.id.widget_favorite_4)
                val tiles = listOf(R.id.widget_tile_1, R.id.widget_tile_2, R.id.widget_tile_3, R.id.widget_tile_4)
                val icons = listOf(R.id.widget_icon_1, R.id.widget_icon_2, R.id.widget_icon_3, R.id.widget_icon_4)
                views.setViewVisibility(R.id.widget_favorites, if (favorites.isEmpty()) View.GONE else View.VISIBLE)
                slots.forEachIndexed { index, viewId ->
                    val source = favorites.getOrNull(index)
                    val tile = tiles[index]
                    // A fixed row weight sum keeps tiles compact; gravity centers only the visible group.
                    views.setViewVisibility(tile, if (source == null) View.GONE else View.VISIBLE)
                    views.setViewVisibility(viewId, if (source == null) View.GONE else View.VISIBLE)
                    if (source != null) {
                        val label = source.title.takeIf { it != Source.labels[source.id] } ?: sourceLabel(context, source.id)
                        val selected = source.id == json.optString("source")
                        val enabled = power == PowerState.ON
                        val color = if (selected) accent else foreground
                        views.setTextViewText(viewId, label)
                        // The existing Spotify artwork already contains its source name.
                        views.setViewVisibility(viewId, if (source.id == "Spotify") View.GONE else View.VISIBLE)
                        views.setTextColor(viewId, if (enabled) color else (color and 0x00FFFFFF) or 0x80000000.toInt())
                        views.setImageViewResource(icons[index], if (source.id == "Spotify") R.drawable.spotify_wordmark else sourceDrawable(source.id))
                        views.setInt(icons[index], "setColorFilter", color)
                        views.setInt(tile, "setBackgroundResource", when {
                            dark && selected -> R.drawable.widget_tile_dark_selected
                            dark -> R.drawable.widget_tile_dark
                            selected -> R.drawable.widget_tile_light_selected
                            else -> R.drawable.widget_tile_light
                        })
                        views.setBoolean(tile, "setEnabled", enabled)
                        views.setInt(icons[index], "setImageAlpha", if (enabled) 255 else 128)
                        views.setContentDescription(tile, label + if (selected) ", " + context.getString(R.string.ui_selected) else "")
                        views.setOnClickPendingIntent(tile, action(base, id, WidgetAction.SOURCE, source.id))
                    }
                }
                manager.updateAppWidget(id, views)
            }
        }
        internal fun action(context: Context, id: Int, action: WidgetAction, source: String? = null): PendingIntent {
            val intent = Intent(context, ReceiverWidget::class.java).setAction(action.name)
                .setData(android.net.Uri.Builder().scheme("yamaha-widget").authority(id.toString()).appendPath(action.name).appendPath(source.orEmpty()).build())
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("source", source)
            return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        private fun sourceLabel(context: Context, id: String): String = context.getString(when(id) {
            "TUNER" -> R.string.tuner; "CD" -> R.string.cd; "OPTICAL" -> R.string.optical; "COAXIAL" -> R.string.coaxial
            "LINE1" -> R.string.line_one; "LINE2" -> R.string.line_two; "LINE3" -> R.string.line_three
            "Spotify" -> R.string.spotify; "SERVER" -> R.string.server; "NET RADIO" -> R.string.net_radio
            "AirPlay" -> R.string.airplay; else -> return id
        })
    }
}

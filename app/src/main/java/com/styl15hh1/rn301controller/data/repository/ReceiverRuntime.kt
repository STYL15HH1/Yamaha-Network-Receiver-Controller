package com.styl15hh1.rn301controller.data.repository

import android.content.Context
import com.styl15hh1.rn301controller.data.network.YamahaHttpClient
import com.styl15hh1.rn301controller.data.settings.*
import com.styl15hh1.rn301controller.widget.ReceiverWidget
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** One repository/transport/command mutex for the activity and widget jobs in this process. */
class ReceiverRuntime private constructor(context: Context) {
    val settings = AppSettings(PreferencesSettingsStore(context))
    val repository = YamahaRepository(YamahaHttpClient(context), PreferencesAddressStore(context), log = {
        if (com.styl15hh1.rn301controller.BuildConfig.DEBUG) android.util.Log.d("YamahaRepository", it)
    })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    init {
        scope.launch {
            combine(repository.status, repository.tuner, repository.player, settings.state) { status, tuner, player, preferences ->
                com.styl15hh1.rn301controller.widget.WidgetSnapshot(status, tuner, player, preferences)
            }.distinctUntilChanged().collect { ReceiverWidget.capture(context, it) }
        }
    }
    companion object {
        @Volatile private var instance: ReceiverRuntime? = null
        fun get(context: Context): ReceiverRuntime = instance ?: synchronized(this) {
            instance ?: ReceiverRuntime(context.applicationContext).also { instance = it }
        }
    }
}

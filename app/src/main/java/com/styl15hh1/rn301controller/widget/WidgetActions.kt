package com.styl15hh1.rn301controller.widget

import com.styl15hh1.rn301controller.data.model.ConnectionState
import com.styl15hh1.rn301controller.data.repository.YamahaRepository
import com.styl15hh1.rn301controller.data.settings.*

/** Uses the normal saved receiver and revalidates favorites immediately before dispatch. */
object WidgetActions {
    suspend fun execute(repo: YamahaRepository, settings: AppSettings, action: WidgetAction, source: String? = null) {
        val address = repo.savedAddress()
        if (address.isBlank()) return
        if (repo.status.value.address != address || repo.status.value.connectionState != ConnectionState.CONNECTED)
            repo.connect(address)
        else repo.refresh()
        if (repo.status.value.connectionState != ConnectionState.CONNECTED || repo.status.value.stale) return
        when (action) {
            WidgetAction.REFRESH -> Unit
            WidgetAction.POWER -> repo.toggleWidgetPower()
            WidgetAction.SOURCE -> {
                val sources = repo.status.value.sources
                val favorites = QuickSources.available(QuickSources.ids(settings.state.value, sources), sources)
                if (favorites.any { it.id == source }) repo.selectSource(source!!)
            }
        }
    }
}

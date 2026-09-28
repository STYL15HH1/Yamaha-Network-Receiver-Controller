package com.styl15hh1.rn301controller.data.model


/** Local presentation only. Original page/line identities are never rewritten. */
object RadioMenuPresentation {
    fun filter(items: List<MediaItem>, query: String): List<MediaItem> =
        query.trim().let { term -> if (term.isEmpty()) items else items.filter { it.title.contains(term, ignoreCase = true) } }

}

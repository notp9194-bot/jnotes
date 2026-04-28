package com.notp9194bot.jnotes.data.model

enum class ViewMode { ALL, PINNED, ARCHIVED, TRASH }
enum class SmartFilter { REMINDER, CHECKLIST, RECENT }

data class FilterParams(
    val view: ViewMode = ViewMode.ALL,
    val search: String = "",
    val sortBy: SortBy = SortBy.UPDATED,
    val activeTag: String? = null,
    val smartFilter: SmartFilter? = null,
    val activeFolderId: String? = null,
)

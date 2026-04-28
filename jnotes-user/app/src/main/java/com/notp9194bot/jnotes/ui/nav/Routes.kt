package com.notp9194bot.jnotes.ui.nav

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{noteId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val LOCK = "lock"
    const val ABOUT = "about"
    const val CALENDAR = "calendar"
    const val CHAT = "chat"
    const val FEEDBACK = "feedback"

    fun editor(noteId: String) = "editor/$noteId"
}

package com.notp9194bot.jnotes.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val type: NoteType,
    val title: String,
    val body: String,
    val items: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val colorIdx: Int = 0,
)

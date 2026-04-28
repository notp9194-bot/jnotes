package com.notp9194bot.jnotes.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean,
)

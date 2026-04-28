package com.notp9194bot.jnotes.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Folder(
    val id: String,
    val name: String,
    val colorIdx: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

fun newFolderId(): String =
    "f_" + java.lang.Long.toString(System.currentTimeMillis(), 36) +
        java.util.UUID.randomUUID().toString().substring(0, 4)

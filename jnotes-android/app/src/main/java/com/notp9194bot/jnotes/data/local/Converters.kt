package com.notp9194bot.jnotes.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTagList(tags: List<String>?): String =
        tags.orEmpty().joinToString(separator = "\u0001")

    @TypeConverter
    fun toTagList(raw: String?): List<String> =
        if (raw.isNullOrEmpty()) emptyList() else raw.split('\u0001').filter { it.isNotEmpty() }
}

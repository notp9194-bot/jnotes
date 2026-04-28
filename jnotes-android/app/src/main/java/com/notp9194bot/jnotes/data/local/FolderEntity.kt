package com.notp9194bot.jnotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notp9194bot.jnotes.data.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("color_idx") val colorIdx: Int,
    @ColumnInfo("created_at") val createdAt: Long,
)

fun FolderEntity.toModel(): Folder = Folder(id = id, name = name, colorIdx = colorIdx, createdAt = createdAt)
fun Folder.toEntity(): FolderEntity = FolderEntity(id = id, name = name, colorIdx = colorIdx, createdAt = createdAt)

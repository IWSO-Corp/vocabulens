package com.iwsocorp.vobynotes.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iwsocorp.vobynotes.core.model.Note

@Entity(tableName = "notes")
class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val wordLang: String,
    val meaningLang: String,
    val contentSize: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val ownerId: String?,
    val shared: Boolean,
)

fun NoteEntity.asExternalModel() = Note(
    id = id,
    ownerId = ownerId,
    title = title,
    wordLang = wordLang,
    meaningLang = meaningLang,
    contentSize = contentSize,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    shared = shared,
)
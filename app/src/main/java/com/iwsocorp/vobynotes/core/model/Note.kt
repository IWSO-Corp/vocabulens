package com.iwsocorp.vobynotes.core.model

import com.iwsocorp.vobynotes.core.database.model.NoteEntity
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val wordLang: String,
    val meaningLang: String,
    val contentSize: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val shared: Boolean = false,
)

fun Note.asEntity() = NoteEntity(
    id = id,
    title = title,
    wordLang = wordLang,
    meaningLang = meaningLang,
    contentSize = contentSize,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    shared = shared,
)
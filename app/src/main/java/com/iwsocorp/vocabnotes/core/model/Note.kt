package com.iwsocorp.vocabnotes.core.model

import com.iwsocorp.vocabnotes.core.database.model.NoteEntity

data class Note(
    val id: String,
    val title: String,
    val wordLang: String,
    val meaningLang: String,
    val contentSize: Int,
    val createdAt: Long,
    val updatedAt: Long
)

fun Note.asEntity() : NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        wordLang = wordLang,
        meaningLang = meaningLang,
        contentSize = contentSize,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
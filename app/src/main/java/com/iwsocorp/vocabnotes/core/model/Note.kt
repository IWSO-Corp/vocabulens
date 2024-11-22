package com.iwsocorp.vocabnotes.core.model

import com.iwsocorp.vocabnotes.core.database.model.NoteEntity

data class Note(
    val id: String,
    val title: String,
    val wordLang: String,
    val meaningLang: String,
    val content: List<Corpus>,
    val createdAt: Long,
    val updatedAt: Long
)

fun Note.asEntity() = NoteEntity(
    id = id,
    title = title,
    wordLang = wordLang,
    meaningLang = meaningLang,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
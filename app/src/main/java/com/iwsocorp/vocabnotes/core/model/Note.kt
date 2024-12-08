package com.iwsocorp.vocabnotes.core.model

import com.google.gson.Gson
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

fun Note.asEntity() : NoteEntity {
    val contentJson = Gson().toJson(content)
    return NoteEntity(
        id = id,
        title = title,
        wordLang = wordLang,
        meaningLang = meaningLang,
        contentJson = contentJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
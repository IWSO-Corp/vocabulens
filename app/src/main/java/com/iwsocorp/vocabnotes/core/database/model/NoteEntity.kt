package com.iwsocorp.vocabnotes.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note

@Entity(tableName = "notes")
class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val wordLang: String,
    val meaningLang: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun NoteEntity.asExternalModel(content: List<Corpus>) = Note(
    id = id,
    title = title,
    wordLang = wordLang,
    meaningLang = meaningLang,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
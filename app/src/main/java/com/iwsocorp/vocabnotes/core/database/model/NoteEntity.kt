package com.iwsocorp.vocabnotes.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note

@Entity(tableName = "notes")
class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val wordLang: String,
    val meaningLang: String,
    val contentJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun NoteEntity.asExternalModel() : Note {
    val content = Gson().fromJson(contentJson, Array<Corpus>::class.java).toList()
    return Note(
        id = id,
        title = title,
        wordLang = wordLang,
        meaningLang = meaningLang,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
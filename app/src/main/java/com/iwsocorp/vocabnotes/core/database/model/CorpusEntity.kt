package com.iwsocorp.vocabnotes.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Meaning

@Entity(tableName = "corpus",)
data class CorpusEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val word: String,
    val meaning: String,
    val wordLang: String,
    val meaningLang: String,
    val phonetic: String,
    val audio: String,
    val meaningsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun CorpusEntity.asExternalModel() : Corpus {
    val meanings = Gson().fromJson(meaningsJson, Array<Meaning>::class.java).toList()
    return Corpus(
        id = id,
        noteId = noteId,
        word = word,
        meaning = meaning,
        wordLang = wordLang,
        meaningLang = meaningLang,
        phonetic = phonetic,
        audio = audio,
        createdAt = createdAt,
        updatedAt = updatedAt,
        meanings = meanings
    )
}
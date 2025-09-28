package com.iwsocorp.vocabnotes.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Mark
import com.iwsocorp.vocabnotes.core.model.Meaning

@Entity(
    tableName = "corpus",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["word", "wordLang", "meaningLang"], unique = true)
    ]
)
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
    val mark: Mark,
    val createdAt: Long,
    val updatedAt: Long,
)

fun CorpusEntity.asExternalModel(): Corpus {
    val meanings = Gson().fromJson(meaningsJson, Array<Meaning>::class.java).toList()
    return Corpus(
        id = id,
        word = word,
        noteId = noteId,
        meaning = meaning,
        wordLang = wordLang,
        meaningLang = meaningLang,
        phonetic = phonetic,
        audio = audio,
        createdAt = createdAt,
        updatedAt = updatedAt,
        meanings = meanings,
        mark = mark,
    )
}
package com.iwsocorp.vocabnotes.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iwsocorp.vocabnotes.core.model.Corpus

@Entity(tableName = "corpus")
data class CorpusEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val word: String,
    val meaning: String,
    val phonetic: String,
    val audio: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun CorpusEntity.asExternalModel(meanings: List<MeaningEntity>) = Corpus(
    id = id,
    noteId = noteId,
    word = word,
    meaning = meaning,
    phonetic = phonetic,
    audio = audio,
    createdAt = createdAt,
    updatedAt = updatedAt,
    meanings = meanings.map { it.asExternalModel() }
)
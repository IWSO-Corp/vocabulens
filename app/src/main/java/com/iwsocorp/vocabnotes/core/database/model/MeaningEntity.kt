package com.iwsocorp.vocabnotes.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iwsocorp.vocabnotes.core.model.Definition
import com.iwsocorp.vocabnotes.core.model.Meaning

@Entity(tableName = "meaning")
class MeaningEntity(
    @PrimaryKey val id: String,
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val synonyms: List<String>,
    val antonyms: List<String>,
)

fun MeaningEntity.asExternalModel(): Meaning {
    val definitions = listOf(
        Definition(
            definition = definition,
            example = example
        )
    )
    return Meaning(
        partOfSpeech = partOfSpeech,
        definitions = definitions,
        synonyms = synonyms,
        antonyms = antonyms
    )
}
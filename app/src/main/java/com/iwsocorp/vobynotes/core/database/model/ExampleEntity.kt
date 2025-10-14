package com.iwsocorp.vobynotes.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iwsocorp.vobynotes.core.model.Example

@Entity(
    tableName = "examples",
    foreignKeys = [
        ForeignKey(
            entity = CorpusEntity::class,
            parentColumns = ["id"],
            childColumns = ["corpusId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sentence"], unique = true)
    ]
)
class ExampleEntity(
    @PrimaryKey val id: String,
    val corpusId: String,
    val forWord: String,
    val sentence: String,
    val createdAt: Long,
    val updatedAt: Long,
    val quizCount: Int,
    val deletedAt: Long? = null,
)

fun ExampleEntity.asExternalModel() = Example(
    id = id,
    corpusId = corpusId,
    forWord = forWord,
    sentence = sentence,
    createdAt = createdAt,
    updatedAt = updatedAt,
    quizCount = quizCount,
)
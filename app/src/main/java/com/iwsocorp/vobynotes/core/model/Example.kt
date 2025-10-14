package com.iwsocorp.vobynotes.core.model

import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import java.util.UUID

data class Example(
    val corpusId: String,
    val forWord: String,
    val sentence: String,
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val quizCount: Int = 0,
)

fun Example.asEntity() = ExampleEntity(
    id = id,
    corpusId = corpusId,
    forWord = forWord,
    sentence = sentence,
    createdAt = createdAt,
    updatedAt = updatedAt,
    quizCount = quizCount,
)
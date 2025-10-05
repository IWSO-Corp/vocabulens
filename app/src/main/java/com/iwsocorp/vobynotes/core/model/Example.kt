package com.iwsocorp.vobynotes.core.model

import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import java.util.UUID

data class Example(
    val forWord: String,
    val sentence: String,
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

fun Example.asEntity() = ExampleEntity(
    id = id,
    forWord = forWord,
    sentence = sentence,
    createdAt = createdAt,
    updatedAt = updatedAt
)
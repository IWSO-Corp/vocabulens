package com.iwsocorp.vocabnotes.core.model

import com.iwsocorp.vocabnotes.core.database.model.ExampleEntity
import java.util.UUID

data class Example(
    val sentence: String,
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

fun Example.asEntity() = ExampleEntity(
    id = id,
    sentence = sentence,
    createdAt = createdAt,
    updatedAt = updatedAt
)
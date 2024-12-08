package com.iwsocorp.vocabnotes.core.model

import com.iwsocorp.vocabnotes.core.database.model.ExampleEntity

data class Example(
    val id: String,
    val example: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun Example.asEntity() = ExampleEntity(
    id = id,
    example = example,
    createdAt = createdAt,
    updatedAt = updatedAt
)
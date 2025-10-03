package com.iwsocorp.vobynotes.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iwsocorp.vobynotes.core.model.Example

@Entity(
    tableName = "examples",
    indices = [
        Index(value = ["sentence"], unique = true)
    ]
)
class ExampleEntity(
    @PrimaryKey val id: String,
    val sentence: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ExampleEntity.asExternalModel() = Example(
    id = id,
    sentence = sentence,
    createdAt = createdAt,
    updatedAt = updatedAt
)
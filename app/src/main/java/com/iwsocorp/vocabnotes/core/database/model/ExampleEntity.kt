package com.iwsocorp.vocabnotes.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iwsocorp.vocabnotes.core.model.Example

@Entity(
    tableName = "examples",
    indices = [
        Index(value = ["example"], unique = true)
    ]
)
class ExampleEntity(
    @PrimaryKey val id: String,
    val example: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ExampleEntity.asExternalModel() = Example(
    id = id,
    example = example,
    createdAt = createdAt,
    updatedAt = updatedAt
)
package com.iwsocorp.vocabnotes.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iwsocorp.vocabnotes.core.database.dao.CorpusDao
import com.iwsocorp.vocabnotes.core.database.dao.ExampleDao
import com.iwsocorp.vocabnotes.core.database.dao.NoteDao
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import com.iwsocorp.vocabnotes.core.database.model.ExampleEntity
import com.iwsocorp.vocabnotes.core.database.model.NoteEntity
import com.iwsocorp.vocabnotes.core.database.util.Converters

@Database(
    entities = [
        NoteEntity::class,
        CorpusEntity::class,
        ExampleEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun corpusDao(): CorpusDao

    abstract fun exampleDao(): ExampleDao

}
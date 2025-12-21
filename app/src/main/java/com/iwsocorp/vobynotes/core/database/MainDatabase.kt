package com.iwsocorp.vobynotes.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iwsocorp.vobynotes.core.database.dao.CorpusDao
import com.iwsocorp.vobynotes.core.database.dao.ExampleDao
import com.iwsocorp.vobynotes.core.database.dao.NoteDao
import com.iwsocorp.vobynotes.core.database.model.CorpusEntity
import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import com.iwsocorp.vobynotes.core.database.model.NoteEntity
import com.iwsocorp.vobynotes.core.database.util.Converters

@Database(
    entities = [
        NoteEntity::class,
        CorpusEntity::class,
        ExampleEntity::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun corpusDao(): CorpusDao

    abstract fun exampleDao(): ExampleDao

}
package com.iwsocorp.vocabnotes.core.database.di

import com.iwsocorp.vocabnotes.core.database.MainDatabase
import com.iwsocorp.vocabnotes.core.database.dao.CorpusDao
import com.iwsocorp.vocabnotes.core.database.dao.ExampleDao
import com.iwsocorp.vocabnotes.core.database.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    fun providesNoteDao(database: MainDatabase): NoteDao = database.noteDao()

    @Provides
    fun providesCorpusDao(database: MainDatabase): CorpusDao = database.corpusDao()

    @Provides
    fun providesExampleDao(database: MainDatabase): ExampleDao = database.exampleDao()

}
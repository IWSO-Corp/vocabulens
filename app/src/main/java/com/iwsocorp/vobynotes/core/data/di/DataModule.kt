package com.iwsocorp.vobynotes.core.data.di

import com.iwsocorp.vobynotes.core.data.repository.AnkiRepository
import com.iwsocorp.vobynotes.core.data.repository.AnkiRepositoryImpl
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepositoryImpl
import com.iwsocorp.vobynotes.core.data.repository.ExampleRepository
import com.iwsocorp.vobynotes.core.data.repository.ExampleRepositoryImpl
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepositoryImpl
import com.iwsocorp.vobynotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vobynotes.core.data.repository.VocabularyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsAnkiRepository(
        ankiRepository: AnkiRepositoryImpl,
    ): AnkiRepository

    @Binds
    internal abstract fun bindsCorpusRepository(
        corpusRepository: CorpusRepositoryImpl,
    ): CorpusRepository

    @Binds
    internal abstract fun bindsMeaningRepository(
        meaningRepository: ExampleRepositoryImpl,
    ): ExampleRepository

    @Binds
    internal abstract fun bindsNoteRepository(
        noteRepository: NoteRepositoryImpl,
    ): NoteRepository

    @Binds
    internal abstract fun bindsVocabularyRepository(
        vocabularyRepository: VocabularyRepositoryImpl,
    ): VocabularyRepository

}
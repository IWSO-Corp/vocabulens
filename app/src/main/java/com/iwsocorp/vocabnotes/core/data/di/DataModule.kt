package com.iwsocorp.vocabnotes.core.data.di

import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepositoryImpl
import com.iwsocorp.vocabnotes.core.data.repository.MeaningRepository
import com.iwsocorp.vocabnotes.core.data.repository.MeaningRepositoryImpl
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepositoryImpl
import com.iwsocorp.vocabnotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vocabnotes.core.data.repository.VocabularyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsCorpusRepository(
        corpusRepository: CorpusRepositoryImpl,
    ): CorpusRepository

    @Binds
    internal abstract fun bindsMeaningRepository(
        meaningRepository: MeaningRepositoryImpl,
    ): MeaningRepository

    @Binds
    internal abstract fun bindsNoteRepository(
        noteRepository: NoteRepositoryImpl,
    ): NoteRepository

    @Binds
    internal abstract fun bindsVocabularyRepository(
        vocabularyRepository: VocabularyRepositoryImpl,
    ): VocabularyRepository

}
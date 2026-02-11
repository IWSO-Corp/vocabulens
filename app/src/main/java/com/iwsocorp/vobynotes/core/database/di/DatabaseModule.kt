package com.iwsocorp.vobynotes.core.database.di

import android.content.Context
import androidx.room.Room
import com.iwsocorp.vobynotes.core.database.MainDatabase
import com.iwsocorp.vobynotes.core.database.migration.MIGRATION_1_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesMainDatabase(
        @ApplicationContext context: Context,
    ): MainDatabase = Room.databaseBuilder(
        context,
        MainDatabase::class.java,
        "main-database"
    )
        .addMigrations(MIGRATION_1_3)
        .build()

}
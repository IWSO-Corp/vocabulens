package com.iwsocorp.vocabnotes.core.network.di

import com.iwsocorp.vocabnotes.core.network.VobyNetworkDataSource
import com.iwsocorp.vocabnotes.core.network.retrofit.RetrofitVobyNetwork
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun providesVobyNetworkDataSource(networkJson: Json): VobyNetworkDataSource {
        return RetrofitVobyNetwork(networkJson)
    }

}
package com.iwsocorp.vocabnotes.core.network.retrofit

import com.iwsocorp.vocabnotes.core.network.VobyNetworkDataSource
import com.iwsocorp.vocabnotes.core.network.model.VocabularyResponseItem
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private interface RetrofitVobyNetworkApi {
    @GET("https://api.dictionaryapi.dev/api/v2/entries/en/{word}")
    suspend fun getVocabulary(
        @Path("word") word: String,
    ): Response<List<VocabularyResponseItem>>
}

@Singleton
internal class RetrofitVobyNetwork @Inject constructor(networkJson: Json) : VobyNetworkDataSource {

    private val networkApi = Retrofit.Builder()
        .baseUrl("https://api.dictionaryapi.dev/api/v2/entries/en/")
        .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RetrofitVobyNetworkApi::class.java)

    override suspend fun getVocabulary(word: String): List<VocabularyResponseItem> {
        return try {
            val response = networkApi.getVocabulary(word)
            Timber.d("response: $response")
            if (response.isSuccessful) {
                response.body() ?: listOf(VocabularyResponseItem())
            } else {
                Timber.e("Failed: ${response.errorBody()}")
                listOf(VocabularyResponseItem())
            }
        } catch (e: Exception) {
            Timber.e(e, "Error occurred: ${e.message}")
            listOf(VocabularyResponseItem())
        }
    }
}
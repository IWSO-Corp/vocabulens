package com.iwsocorp.vobynotes.core.network.retrofit

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

interface GoogleFormService {

    @FormUrlEncoded
    @POST("formResponse")
    suspend fun postFeedback(
        @Field("emailAddress") email: String,
        @Field("entry.1966404122") feedback: String
    ): Response<Void>
}

@Singleton
class GoogleFormRepository @Inject constructor(networkJson: Json) {

    private val formId = "1FAIpQLSeCSfxv-BHU78uBn8IEKLSHbQZAOXUnVX_bQXCr1Xzbi3y5DQ"
    private val formUrl = "https://docs.google.com/forms/d/e/$formId/"
    private val service = Retrofit.Builder()
        .baseUrl(formUrl)
        .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GoogleFormService::class.java)

    suspend fun postFeedback(email: String, feedback: String): Response<Void> {
        return service.postFeedback(email, feedback)
    }

}

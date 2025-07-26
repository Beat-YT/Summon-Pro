package com.justjdupuis.summonpro.api

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST

object AuthApi {
    private const val BASE_URL = "https://gate.summon-pro.cc/api/auth/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    data class LoginResponse(
        @SerializedName("encrypted_token") val encryptedToken: String,
        @SerializedName("encrypted_refresh_token") val encryptedRefreshToken: String,
        @SerializedName("id_token") val idToken: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("token_type") val tokenType: String
    )
    interface Service {
        @FormUrlEncoded
        @POST("login")
        suspend fun loginWithTeslaCode(
            @Field("tesla_code") code: String,
            @Field("redirect_uri") uri: String
        ): LoginResponse

        @FormUrlEncoded
        @POST("refresh")
        suspend fun refreshWithToken(
            @Field("encrypted_refresh_token") refresh: String,
        ): LoginResponse

        @GET("verify")
        suspend fun verifyToken(
            @Header("Authorization") token: String
        ): LoginResponse
    }

    val service: Service = retrofit.create(Service::class.java)
}
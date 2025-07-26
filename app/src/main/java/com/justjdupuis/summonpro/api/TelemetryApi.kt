package com.justjdupuis.summonpro.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

object TelemetryApi {
    private const val BASE_URL = "https://gate.summon-pro.cc/api/telemetry/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    data class RegistrationResponse(
        val serviceUrl: String,
        val serviceToken: String,
    )

    data class TelemetryInfoResponse(
        val synced: Boolean,
        @SerializedName("limit_reached") val limitReached: Boolean,
        @SerializedName("key_paired") val keyPaired: Boolean,
    )

    interface Service {
        @POST("{VIN}")
        suspend fun registerTelemetry(
            @Header("Authorization") token: String,
            @Path("VIN") vin: String,
        ): RegistrationResponse

        @GET("{VIN}")
        suspend fun getTelemetryInfo(
            @Header("Authorization") token: String,
            @Path("VIN") vin: String,
        ): TelemetryInfoResponse

        @DELETE("{VIN}")
        suspend fun unregisterTelemetry(
            @Header("Authorization") token: String,
            @Path("VIN") vin: String,
        ): Response<Unit>
    }

    val service: Service = retrofit.create(Service::class.java)
}
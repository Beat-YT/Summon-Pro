package com.justjdupuis.summonpro.api

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

object TeslaApi {
    private const val BASE_URL = "http://192.168.2.17:8080/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    interface Service {
        @GET("tesla/api/1/vehicles")
        suspend fun getVehicleList(
            @Header("Authorization") token: String,
        ): VehicleListResponse

        @GET("tesla/api/1/vehicles/{vehicle_tag}")
        suspend fun getVehicleInfo(
            @Header("Authorization") token: String,
            @Path("vehicle_tag") vehicleTag: String,
        ): VehicleResponse
    }

    val service: Service = retrofit.create(Service::class.java)

    data class VehicleListResponse(
        val response: List<Vehicle>,
        val count: Int
    )

    data class VehicleResponse(val response: Vehicle)

    data class Vehicle(
        @SerializedName("id") val id: Long,
        @SerializedName("vehicle_id") val vehicleId: Long,
        @SerializedName("vin") val vin: String,
        @SerializedName("color") val color: Any?, // can be null
        @SerializedName("access_type") val accessType: String,
        @SerializedName("display_name") val displayName: String,
        @SerializedName("option_codes") val optionCodes: String,
        @SerializedName("granular_access") val granularAccess: Any?,
        @SerializedName("tokens") val tokens: Any?, // use Any? or JsonObject if mixed/unknown
        @SerializedName("state") val state: String,
        @SerializedName("in_service") val inService: Boolean,
        @SerializedName("id_s") val idS: String,
        @SerializedName("calendar_enabled") val calendarEnabled: Boolean,
        @SerializedName("api_version") val apiVersion: Int,
        @SerializedName("backseat_token") val backseatToken: Any?,
        @SerializedName("backseat_token_updated_at") val backseatTokenUpdatedAt: Any?,
        @SerializedName("ble_autopair_enrolled") val bleAutopairEnrolled: Boolean
    )
}
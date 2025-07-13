package com.justjdupuis.tesla_summonpro

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

object TeslaApi {
    private const val BASE_URL = "https://fleet-api.prd.na.vn.cloud.tesla.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private interface Service {
        @GET("api/1/vehicles/{car_id}/vehicle_data")
        suspend fun getVehicleData(
            @Header("Authorization") auth: String,
            @Path("car_id") carId: String,
            @Query("endpoints") endpoints: String = "location_data"
        ): VehicleResponse
    }

    private val service = retrofit.create(Service::class.java)

    suspend fun getDriveState(bearerToken: String, carId: String): DriveState =
        service
            .getVehicleData("Bearer $bearerToken", carId)
            .response
            .driveState

    data class VehicleResponse(val response: ResponseBody)
    data class ResponseBody(
        @SerializedName("drive_state")
        val driveState: DriveState
    )
    data class DriveState(val latitude: Double, val longitude: Double)
}
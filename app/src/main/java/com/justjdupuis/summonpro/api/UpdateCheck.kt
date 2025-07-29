package com.justjdupuis.summonpro.api

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

object UpdateCheck {
    private const val BASE_URL = "https://summon-pro.cc/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    data class ClientInfo(
        val latestVersion: String,
        val minSupportedVersion: String,
        val updateUrl: String,
        val title: String,
        val changelog: String,
        val required: Boolean
    )

    interface Service {
        @GET(".well-known/client.json")
        suspend fun getClientInfo(): ClientInfo
    }

    private val service: Service = retrofit.create(Service::class.java)
    private fun isVersionOlder(current: String, target: String): Boolean {
        val curParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val targetParts = target.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(curParts.size, targetParts.size)) {
            val c = curParts.getOrElse(i) { 0 }
            val t = targetParts.getOrElse(i) { 0 }
            if (c < t) return true
            if (c > t) return false
        }
        return false
    }

    suspend fun needUpdate(context: Context): ClientInfo? {
        try {
            val clientInfo = service.getClientInfo()
            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName

            if (isVersionOlder(currentVersion, clientInfo.minSupportedVersion)) {
                return clientInfo
            }

            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null // fallback to "no update needed" on failure
        }
    }
}
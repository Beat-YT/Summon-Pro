package com.justjdupuis.summonpro.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.justjdupuis.summonpro.R
import com.justjdupuis.summonpro.SummonForegroundService
import okhttp3.ResponseBody


object Carpenter {
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "nagger",
                "Summon Fake Location Service",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(false) // foreground services don't need badges
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    data class ApiError(
        @SerializedName("error_message") val message: String,
        @SerializedName("error_code") val code: String
    )

    fun parseApiError(errorBody: ResponseBody?): ApiError? {
        return try {
            val gson = Gson()
            gson.fromJson(errorBody?.charStream(), ApiError::class.java)
        } catch (e: Exception) {
            null
        }
    }


    fun buildNotification(context: Context): Notification {
        val stopIntent = Intent(context, SummonForegroundService::class.java).apply {
            action = "STOP_SERVICE"
        }

        val stopPendingIntent = PendingIntent.getService(
            context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        createNotificationChannel(context)
        return NotificationCompat.Builder(context, "nagger")
            .setContentTitle("SummonPro Running...")
            .setContentText("SummonPro service is active")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(0, "Stop", stopPendingIntent)
            .setSmallIcon(R.drawable.ic_autopilot)
            .build()
    }

    fun decodeTeslaVin(vin: String): String {
        if (vin.length != 17) return "Invalid VIN"

        val yearCodeMap = mapOf(
            'A' to 2010, 'B' to 2011, 'C' to 2012, 'D' to 2013, 'E' to 2014,
            'F' to 2015, 'G' to 2016, 'H' to 2017, 'J' to 2018, 'K' to 2019,
            'L' to 2020, 'M' to 2021, 'N' to 2022, 'P' to 2023, 'R' to 2024,
            'S' to 2025, 'T' to 2026, 'V' to 2027, 'W' to 2028, 'X' to 2029,
            'Y' to 2030
        )

        val modelCodeMap = mapOf(
            'S' to "Model S",
            '3' to "Model 3",
            'X' to "Model X",
            'Y' to "Model Y",
            'C' to "Cybertruck",
            'T' to "Semi"
        )

        val yearCode = vin[9].uppercaseChar()
        val modelCode = vin[3].uppercaseChar()

        val year = yearCodeMap[yearCode] ?: return "New"
        val model = modelCodeMap[modelCode] ?: "Unknown Model"

        return "$year $model"
    }
}
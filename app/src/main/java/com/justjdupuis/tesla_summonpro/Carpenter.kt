package com.justjdupuis.tesla_summonpro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import androidx.core.app.NotificationCompat


object Carpenter {
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "nagger",
                "Nagger Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
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
            .addAction(0, "Stop", stopPendingIntent)
            .setSmallIcon(R.drawable.ic_autopilot) // make sure this exists
            .build()
    }

    data class Point(val x: Double, val y: Double)
    fun clampToCircle(center: Point, target: Point, radius: Double): Point {
        val dx = target.x - center.x
        val dy = target.y - center.y
        val dist2 = dx * dx + dy * dy
        return if (dist2 <= radius * radius) {
            target
        } else {
            val dist = kotlin.math.sqrt(dist2)
            val scale = radius / dist
            Point(
                x = center.x + dx * scale,
                y = center.y + dy * scale
            )
        }
    }
}
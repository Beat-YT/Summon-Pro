package com.justjdupuis.summonpro.utils

import android.content.Context

object LocationStore {
    private const val PREFS_NAME = "summonpro_location"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_HEADING = "heading"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLocation(ctx: Context, latitude: Double, longitude: Double) {
        prefs(ctx).edit()
            .putString(KEY_LATITUDE, latitude.toString())
            .putString(KEY_LONGITUDE, longitude.toString())
            .apply()
    }

    fun saveHeading(ctx: Context, heading: Double) {
        prefs(ctx).edit()
            .putString(KEY_HEADING, heading.toString())
            .apply()
    }

    fun getLatitude(ctx: Context): Double? {
        return prefs(ctx).getString(KEY_LATITUDE, null)?.toDoubleOrNull()
    }

    fun getLongitude(ctx: Context): Double? {
        return prefs(ctx).getString(KEY_LONGITUDE, null)?.toDoubleOrNull()
    }

    fun getHeading(ctx: Context): Double? {
        return prefs(ctx).getString(KEY_HEADING, null)?.toDoubleOrNull()
    }
}
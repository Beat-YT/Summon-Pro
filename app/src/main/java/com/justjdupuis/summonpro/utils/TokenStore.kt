// TokenStore.kt

package com.justjdupuis.summonpro.utils

import android.content.Context

object TokenStore {
    private const val PREFS_NAME = "summonpro_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EXPIRES = "expires_at"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(ctx: Context, accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        prefs(ctx).edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putLong(KEY_EXPIRES, System.currentTimeMillis() + expiresInSeconds * 1000)
            .apply()
    }

    fun getAccessToken(ctx: Context): String? {
        val p = prefs(ctx)
        val expiresAt = p.getLong(KEY_EXPIRES, 0)
        val earlyOffset = 4 * 60 * 60 * 1000L
        return if (System.currentTimeMillis() >= (expiresAt - earlyOffset)) {
            null
        } else {
            p.getString(KEY_ACCESS, null)
        }
    }

    fun getRefreshToken(ctx: Context): String? =
        prefs(ctx).getString(KEY_REFRESH, null)

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    fun isExpired(ctx: Context): Boolean =
        System.currentTimeMillis() >= prefs(ctx).getLong(KEY_EXPIRES, 0)
}

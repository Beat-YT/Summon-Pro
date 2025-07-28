import android.content.Context
import android.util.Log
import com.justjdupuis.summonpro.api.AuthApi
import com.justjdupuis.summonpro.utils.TokenStore

object TokenManager {
    private const val EARLY_OFFSET_MS = 4 * 60 * 60 * 1000L // 4 hours

    suspend fun getValidAccessToken(ctx: Context): String? {
        val token = TokenStore.getAccessToken(ctx)
        if (token != null) return token

        // If it's already expired, try to refresh
        val refreshToken = TokenStore.getRefreshToken(ctx) ?: return null

        return try {
            val result = AuthApi.service.refreshWithToken(refreshToken)
            TokenStore.save(ctx, result.encryptedToken, result.encryptedRefreshToken, result.expiresIn)
            result.encryptedToken
        } catch (e: Exception) {
            clearSession(ctx)
            Log.e("TokenManager", "Token refresh failed", e)
            null
        }
    }

    fun hasValidAccessToken(ctx: Context): Boolean {
        return TokenStore.getAccessToken(ctx) != null
    }

    fun clearSession(ctx: Context) {
        TokenStore.clear(ctx)
    }
}

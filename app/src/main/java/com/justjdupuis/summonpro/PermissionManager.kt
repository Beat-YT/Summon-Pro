package com.justjdupuis.summonpro  // adjust to your package

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts

class PermissionManager(caller: ActivityResultCaller) {
    private var callback: ((Boolean) -> Unit)? = null

    private val permissionLauncher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        callback?.invoke(granted)
    }

    /**
     * @param permission e.g. Manifest.permission.ACCESS_FINE_LOCATION
     * @param onResult called with true if granted
     */
    fun request(permission: String, onResult: (Boolean) -> Unit) {
        callback = onResult
        permissionLauncher.launch(permission)
    }

    companion object {
        fun isGranted(ctx: Context, permission: String): Boolean =
            ContextCompat.checkSelfPermission(ctx, permission) ==
                    PackageManager.PERMISSION_GRANTED
    }
}

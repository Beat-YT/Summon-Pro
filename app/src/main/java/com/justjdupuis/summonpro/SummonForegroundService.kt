package com.justjdupuis.summonpro

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.justjdupuis.summonpro.api.TelemetryApi
import com.justjdupuis.summonpro.api.WebSocketManager
import com.justjdupuis.summonpro.models.TelemetryConn
import com.justjdupuis.summonpro.utils.Carpenter
import com.justjdupuis.summonpro.utils.GeoHelper
import com.justjdupuis.summonpro.utils.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class SummonForegroundService : Service(), WebSocketManager.WebSocketEventListener {
    companion object {
        internal var isRunning = false
        private const val TAG = "SummonForegroundService"
        private const val PROVIDER = LocationManager.GPS_PROVIDER
        private const val MOCK_INTERVAL_MS = 500L
        const val ACTION_STOP_SERVICE = "com.justjdupuis.summonpro.action.STOP_SERVICE"
        const val EXTRA_LOCATION_LAT = "extra_location_lat"
        const val EXTRA_LOCATION_LNG = "extra_location_lng"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var locationManager: LocationManager
    private val mockLocation = Location(PROVIDER)
    private var screenOffReceiver: BroadcastReceiver? = null

    private var geofenceRadius = 80.0
    private var distanceToClaim = 20.0

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        initMockProvider()
        registerScreenOffReceiver()
        WebSocketManager.addListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }


        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        distanceToClaim = prefs.getString("claim_distance", "20")?.toDoubleOrNull() ?: 20.0
        geofenceRadius = when (prefs.getString("mock_mode", "china_na")) {
            "global" -> 5.8
            "china_na" -> 80.0
            else -> 85.0
        }

        startForeground(1, Carpenter.buildNotification(this))
        onNewLocation(WebSocketManager.latitude!!, WebSocketManager.longitude!!)

        startMockLoop()
        Toast.makeText(this, "Summon service started", Toast.LENGTH_SHORT).show()

        isRunning = true;
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initMockProvider() {
        runCatching {
            locationManager.addTestProvider(
                PROVIDER,
                false, false, false, false,
                true, true, true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
        }
        locationManager.setTestProviderEnabled(PROVIDER, true)
    }

    private fun removeMockProvider() {
        locationManager.setTestProviderEnabled(PROVIDER, false)
        locationManager.removeTestProvider(PROVIDER)
    }

    private fun registerScreenOffReceiver() {
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    handleScreenOff()
                }
            }
        }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun unregisterScreenOffReceiver() {
        screenOffReceiver?.let { unregisterReceiver(it) }
        screenOffReceiver = null
    }

    private fun handleScreenOff() {
        serviceScope.launch {
            WebSocketManager.close()
            unregisterTelemetry()
            stopSelf()
        }
    }

    private suspend fun unregisterTelemetry() {
        runCatching {
            val token = TokenStore.getAccessToken(this@SummonForegroundService) ?: return
            TelemetryApi.service.unregisterTelemetry(token, WebSocketManager.vin.orEmpty())
        }.onFailure { Log.e(TAG, "Unregister telemetry error", it) }
    }

    private fun startMockLoop() {
        serviceScope.launch {
            while (isActive) {
                pushMockLocation()
                delay(MOCK_INTERVAL_MS)
            }
        }
    }

    private fun pushMockLocation() {
        mockLocation.apply {
            accuracy = 1f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        locationManager.setTestProviderLocation(PROVIDER, mockLocation)
    }

    override fun onOpen() {
    }

    override fun onNewLocation(latitude: Double, longitude: Double) {
        val center = LatLng(latitude, longitude)
        val path = FirstFragment.pathPoints
        while (path.size > 1 && SphericalUtil.computeDistanceBetween(
                center,
                path.first()
            ) <= distanceToClaim
        ) {
            path.removeFirst()
        }

        val target = path.firstOrNull() ?: return
        val inside = GeoHelper.clampToCircle(center, target, geofenceRadius)
        mockLocation.latitude = inside.latitude
        mockLocation.longitude = inside.longitude
        pushMockLocation()
    }

    override fun onNewHeading(heading: Double) {
    }

    override fun onConnectivityUpdate(connectivity: TelemetryConn) {
        // TODO update the user with a notification

    }

    override fun onClosed() {
    }

    override fun onFailure(t: Throwable) {
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Service destroyed")
        serviceScope.cancel()
        WebSocketManager.removeListener(this)
        unregisterScreenOffReceiver()
        removeMockProvider()
    }
}
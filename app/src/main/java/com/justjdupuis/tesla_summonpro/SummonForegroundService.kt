package com.justjdupuis.tesla_summonpro

import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*

class SummonForegroundService : Service() {
    private lateinit var locationManager: LocationManager
    private val mockLocation = Location(LocationManager.GPS_PROVIDER)
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InFEc3NoM2FTV0cyT05YTTdLMzFWV0VVRW5BNCJ9.eyJpc3MiOiJodHRwczovL2ZsZWV0LWF1dGgudGVzbGEuY29tL29hdXRoMi92My9udHMiLCJhenAiOiI0M2I5Y2M0Mi1lMjdhLTRkYTctYTdlMi02ZTkzYjgxNGRkM2IiLCJzdWIiOiI0YTg0N2U2Zi1mOWE5LTRhZTEtYmU2Yi04M2M4MGMwYmRkYjMiLCJhdWQiOlsiaHR0cHM6Ly9mbGVldC1hcGkucHJkLm5hLnZuLmNsb3VkLnRlc2xhLmNvbSIsImh0dHBzOi8vZmxlZXQtYXBpLnByZC5ldS52bi5jbG91ZC50ZXNsYS5jb20iLCJodHRwczovL2ZsZWV0LWF1dGgudGVzbGEuY29tL29hdXRoMi92My91c2VyaW5mbyJdLCJzY3AiOlsidmVoaWNsZV9sb2NhdGlvbiIsInZlaGljbGVfZGV2aWNlX2RhdGEiXSwiYW1yIjpbInB3ZCIsIm1mYSIsIm90cCJdLCJleHAiOjE3NTIzNzA3NzUsImlhdCI6MTc1MjM0MTk3NSwib3VfY29kZSI6Ik5BIiwibG9jYWxlIjoiZW4tQ0EiLCJhY2NvdW50X3R5cGUiOiJwZXJzb24iLCJvcGVuX3NvdXJjZSI6ZmFsc2UsImFjY291bnRfaWQiOiJhZGUxNGU2Ni1iM2Q0LTQzMDEtYmY3OC00MTMzMjQ3OWNmMTciLCJhdXRoX3RpbWUiOjE3NTIzNDE5MzJ9.bAicQ2GSzDXrwq1nQvLGqz7aeeOtbGlCy-t8Na1h9zcldNiEun8_vmgEgrTUJo2bgJZgmVA6hB4s9TbO2R_VrO20XW6xHAxZThAAGIST5mvFSg9zgIdnd04ZSGTkMghhJvbRMhoLfzerpwtNQWfWHUvcdzxMZknWYPNNMYKw2E-V_OoPnmYf89nm-NbnQUunwit33FI2iFAr3KpJLk_Cfw0d_PNirbx5qLj7UMtXdvkqk0lSXHIcVNK5Ui6k3H60t-BkETG6OiGq7N1g7w53I6eMyCL4eZEZmG8iEIY5O8MK5ov2oCLDT9NiizE5BOA_9sHt5nkQXwNQvz9wOax6RQ"
    private val carId = "1492931462696231"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1, Carpenter.buildNotification(this))
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        registerMockProvider()

        startMockPushLoop()
        startTeslaFetchLoop()

        mockLocation.latitude = 45.301993;
        mockLocation.longitude = -73.262817;

        Toast.makeText(this, "Summon service started", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    private fun startMockPushLoop() {
        serviceScope.launch {
            while (isActive) {
                pushMockLocation()
                delay(1000)
            }
        }
    }

    private fun startTeslaFetchLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val ds = TeslaApi.getDriveState(token, carId)

                    val A = Carpenter.Point(ds.latitude, ds.longitude)
                    val B = Carpenter.Point(FirstFragment.lastLatInput, FirstFragment.lastLonInput)

                    val metresPerDeg = 111_319.9
                    val Rdeg = 80 / metresPerDeg
                    val P = Carpenter.clampToCircle(A, B, Rdeg)

                    mockLocation.latitude = P.x
                    mockLocation.longitude = P.y
                    pushMockLocation();
                } catch (e: Exception) {
                    Log.e("SummonService", "Failed to fetch Tesla GPS", e)
                }
                delay(5000)
            }
        }
    }

    private fun pushMockLocation() {
        mockLocation.apply {
            accuracy = 1f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
    }

    private fun registerMockProvider() {
        try {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false,
                true, true, true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
        } catch (_: Exception) { }
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
        locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

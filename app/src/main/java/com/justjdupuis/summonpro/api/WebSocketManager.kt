package com.justjdupuis.summonpro.api

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.justjdupuis.summonpro.models.TelemetryConn
import com.justjdupuis.summonpro.models.TelemetryV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

object WebSocketManager {

    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null

    private var isManuallyClosed = false

    private val listeners = mutableSetOf<WeakReference<WebSocketEventListener>>()

    private lateinit var  lastUrl: String;
    private lateinit var lastToken: String;

    private var lastMessageTime = System.currentTimeMillis()
    private var watchdogJob: Job? = null

    var latitude: Double? = null
    var longitude: Double? = null
    var vin: String? = null

    interface WebSocketEventListener {
        fun onOpen()
        fun onNewLocation(latitude: Double, longitude: Double)
        fun onNewHeading(heading: Double)
        fun onConnectivityUpdate(connectivity: TelemetryConn)
        fun onClosed()
        fun onFailure(t: Throwable)
    }

    fun connect(url: String, token: String) {
        if (webSocket != null) return

        isManuallyClosed = false
        client = OkHttpClient.Builder()
            .pingInterval(5, TimeUnit.SECONDS)
            .build()

        lastUrl = url;
        lastToken = token;

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", token)
            .build()

        webSocket = client!!.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("WebSocketManager", "onOpen")
                lastMessageTime = System.currentTimeMillis()
                startWatchdog()
                listeners.forEach { it.get()?.onOpen() }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("WebSocketManager", "onMessage: $text")
                lastMessageTime = System.currentTimeMillis()

                // parse the message
                val root = JSONObject(text)
                val topic = root.getString("topic")

                if (topic == "V") {
                    val contentJson = root.getJSONObject("content").toString()
                    val telemetry = Gson().fromJson(contentJson, TelemetryV::class.java)

                    val location = telemetry.data
                        .firstOrNull { it.key == "Location" }
                        ?.value
                        ?.locationValue

                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        listeners.forEach { it.get()?.onNewLocation(location.latitude, location.longitude) }
                    }

                    val heading = telemetry.data
                        .firstOrNull { it.key == "GpsHeading" }
                        ?.value
                        ?.doubleValue

                    if (heading != null) {
                        listeners.forEach { it.get()?.onNewHeading(heading) }
                    }

                    vin = telemetry.vin;
                } else if (topic == "connectivity" || topic == "cached_connectivity") {
                    val contentJson = root.getJSONObject("content").toString()
                    val connectivity = Gson().fromJson(contentJson, TelemetryConn::class.java)
                    listeners.forEach { it.get()?.onConnectivityUpdate(connectivity) }
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketManager", "onClosed: $reason")
                listeners.forEach { it.get()?.onClosed() }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketManager", "onFailure", t)
                listeners.forEach { it.get()?.onFailure(t) }
                if (!isManuallyClosed) {
                    reconnect(url, token)
                }
            }
        })
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun close() {
        isManuallyClosed = true
        webSocket?.close(1000, null)
        webSocket = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
        stopWatchdog()
    }

    fun shutdown() {
        close()
        listeners.clear()
    }

    fun isConnected(): Boolean = webSocket != null

    fun addListener(listener: WebSocketEventListener) {
        listeners.add(WeakReference(listener))
    }

    fun removeListener(listener: WebSocketEventListener) {
        listeners.removeAll { it.get() == listener || it.get() == null }
    }

    private fun reconnect(url: String, token: String) {
        try {
            webSocket?.close(4001, "reconnecting")
        } catch (e: Exception) {}

        webSocket = null

        CoroutineScope(Dispatchers.IO).launch {
            delay(3000) // 3-second delay before reconnecting
            connect(url, token)
        }
    }

    private fun startWatchdog(timeoutMs: Long = 25000L) {
        watchdogJob?.cancel()
        watchdogJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(7000)

                val timeSince = System.currentTimeMillis() - lastMessageTime
                if (timeSince > timeoutMs) {
                    Log.w("WebSocketManager", "Watchdog: No message in ${timeSince}ms, reconnecting.")
                    reconnect(lastUrl, lastToken)
                } else {
                    try {
                        webSocket?.send("watchdog")
                    } catch (e: Exception){}
                }
            }
        }
    }

    private fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }
}

package com.justjdupuis.summonpro

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.justjdupuis.summonpro.api.TelemetryApi
import com.justjdupuis.summonpro.api.WebSocketManager
import com.justjdupuis.summonpro.databinding.FragmentFirstBinding
import com.justjdupuis.summonpro.models.TelemetryConn
import com.justjdupuis.summonpro.utils.Carpenter
import com.justjdupuis.summonpro.utils.LocationStore
import com.justjdupuis.summonpro.utils.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    WebSocketManager.WebSocketEventListener {

    private lateinit var permMgr: PermissionManager
    private var _binding: FragmentFirstBinding? = null
    private lateinit var map: GoogleMap

    private var vehicleMarker: Marker? = null
    private var isConnected: Boolean = false


    private val markerPoints = mutableListOf<Marker>()
    private var polyline: Polyline? = null

    companion object {
        val pathPoints = mutableListOf<LatLng>()
    }


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        permMgr = PermissionManager(this)

        requireActivity().onBackPressedDispatcher.addCallback(requireActivity()) {
            if (SummonForegroundService.isRunning) {
                Toast.makeText(requireContext(), "Stop Summon Pro before leaving.", Toast.LENGTH_SHORT).show()
            } else {
                isEnabled = false
                findNavController().popBackStack()
            }
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vin = arguments?.getString("vin")
        if (vin == null) {
            Toast.makeText(requireContext(), "Error VIN not provided", Toast.LENGTH_SHORT).show()
            return
        }

        WebSocketManager.vin = vin
        binding.btnUndo.setOnClickListener {
            if (SummonForegroundService.isRunning) {
                Toast.makeText(requireContext(), "Stop Summon Service to remove path", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            if (pathPoints.isNotEmpty()) {
                pathPoints.removeLast()
                polyline?.points = pathPoints

                if (markerPoints.isNotEmpty()) {
                    val marker = markerPoints.removeLast()
                    marker.remove()
                }
            }
        }

        val mapFrag = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)

        val token = TokenStore.getAccessToken(requireContext());
        if (token == null) {
            Toast.makeText(requireContext(), "Error no access token found", Toast.LENGTH_SHORT)
                .show()
            return
        }

        lifecycleScope.launch {
            try {
                val registrationService = TelemetryApi.service.registerTelemetry(token, vin)
                WebSocketManager.close()
                WebSocketManager.connect(
                    registrationService.serviceUrl,
                    registrationService.serviceToken
                )
            } catch (e: Exception) {
                showAlert(
                    "Cannot stream GPS",
                    "Sorry but we were unable to setup telemetry streaming from your vehicle."
                )
                Log.e("FirstFragment", "onViewCreated failed to registerTelemetry", e)
            }
        }
    }

    fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.uiSettings.isTiltGesturesEnabled = false
        map.setOnMapClickListener(this)

        @SuppressLint("MissingPermission")
        if (PermissionManager.isGranted(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        } else {
            permMgr.request(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
                if (granted) {
                    map.isMyLocationEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = true
                }
            }
        }

        val latitude = LocationStore.getLatitude(requireContext()) ?: 45.5017
        val longitude = LocationStore.getLongitude(requireContext()) ?: -73.5673
        val heading = LocationStore.getHeading(requireContext()) ?: 0.5

        // move camera somewhere
        val defaultLoc = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 18f))

        val orig = BitmapFactory.decodeResource(resources, R.drawable.vehicle_mapmarker_stale)
        val size = dpToPx(48)
        val smallBmp = Bitmap.createScaledBitmap(orig, size, size, false)
        val icon = BitmapDescriptorFactory.fromBitmap(smallBmp)

        vehicleMarker = map.addMarker(
            MarkerOptions()
                .position(defaultLoc)
                .icon(icon)
                .anchor(0.5f, 0.5f)
                .rotation(heading.toFloat())
                .flat(true)
        )
    }

    override fun onMapClick(point: LatLng) {
        pathPoints.add(point)

        if (polyline == null) {
            polyline = map.addPolyline(
                PolylineOptions()
                    .addAll(pathPoints)
                    .color(Color.BLUE)
                    .width(5f)
            )
        } else {
            polyline!!.points = pathPoints
        }

        // Optional: drop a marker at the clicked point
        val marker = map.addMarker(MarkerOptions().position(point))
        marker?.let { markerPoints.add(it) }

        /*lastLatInput = point.latitude
        lastLonInput = point.longitude

        if (targetMarker != null) {
            targetMarker!!.position = point
            return
        }

        val icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

        targetMarker = map.addMarker(
            MarkerOptions()
                .position(point)
                .icon(icon)
                .draggable(true)
        )

        // Optional: move or zoom camera
        map.animateCamera(CameraUpdateFactory.newLatLng(point))*/
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        if (!SummonForegroundService.isRunning && WebSocketManager.isConnected()) {
            WebSocketManager.close()
            CoroutineScope(Dispatchers.IO).launch {
                unregisterTelemetry()
            }
        }
    }

    override fun onStart() {
        WebSocketManager.addListener(this)
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        WebSocketManager.removeListener(this)
    }

    override fun onOpen() {
        requireActivity().runOnUiThread {
            binding.statusDot.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.status_circle_yellow)
            binding.statusText.text = "Connecting"
        }
    }

    override fun onNewLocation(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)

        if (!SummonForegroundService.isRunning) {
            LocationStore.saveLocation(requireContext(), latitude, longitude)
        }

        isConnected = true
        requireActivity().runOnUiThread {
            vehicleMarker?.position = latLng
            if (!isConnected) {
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                updateVehicleIcon(true)
            }

            binding.statusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.status_circle_green)
            binding.statusText.text = "Connected"
        }
    }

    override fun onNewHeading(heading: Double) {
        requireActivity().runOnUiThread {
            vehicleMarker?.rotation = heading.toFloat()
        }

        if (!SummonForegroundService.isRunning) {
            LocationStore.saveHeading(requireContext(), heading)
        }
    }

    override fun onConnectivityUpdate(connectivity: TelemetryConn) {
        isConnected = connectivity.status == "CONNECTED"

        requireActivity().runOnUiThread {
            updateVehicleIcon(isConnected)

            val statusDrawable = if (isConnected) {
                R.drawable.status_circle_green
            } else {
                R.drawable.status_circle_red
            }

            binding.statusDot.background = ContextCompat.getDrawable(requireContext(), statusDrawable)
            binding.statusText.text = if (isConnected) "Connected" else "Disconnected"
        }
    }

    override fun onClosed() {
        requireActivity().runOnUiThread {
            binding.statusDot.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.status_circle_red)
            binding.statusText.text = "Disconnected"
            isConnected = false
            updateVehicleIcon(false)
        }
    }

    override fun onFailure(t: Throwable) {
        requireActivity().runOnUiThread {
            binding.statusDot.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.status_circle_red)
            binding.statusText.text = "Failure"
            isConnected = false
            updateVehicleIcon(false)
        }
    }

    private fun updateVehicleIcon(online: Boolean) {
        val orig = BitmapFactory.decodeResource(
            resources,
            if (online) R.drawable.vehicle_mapmarker_online else R.drawable.vehicle_mapmarker_stale
        )

        val size = dpToPx(48)
        val smallBmp = Bitmap.createScaledBitmap(orig, size, size, false)
        val icon = BitmapDescriptorFactory.fromBitmap(smallBmp)
        vehicleMarker?.setIcon(icon)
    }

    private suspend fun unregisterTelemetry() {
        runCatching {
            val token = TokenStore.getAccessToken(requireContext()) ?: return
            TelemetryApi.service.unregisterTelemetry(token, WebSocketManager.vin.orEmpty())
        }.onFailure { Log.e("MainActivity", "Unregister telemetry error", it) }
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
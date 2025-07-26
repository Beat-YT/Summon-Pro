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
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
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
import com.justjdupuis.summonpro.utils.Carpenter
import com.justjdupuis.summonpro.utils.TokenStore
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
    private var targetMarker: Marker? = null


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


        // move camera somewhere:
        val defaultLoc = LatLng(45.5017, -73.5673)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 16f))

        val orig = BitmapFactory.decodeResource(resources, R.drawable.vehicle_mapmarker_online)

        val size = dpToPx(48)
        val smallBmp = Bitmap.createScaledBitmap(orig, size, size, false)

        val icon = BitmapDescriptorFactory.fromBitmap(smallBmp)
        val pt = LatLng(45.5017, -73.5673)
        vehicleMarker = map.addMarker(
            MarkerOptions()
                .position(pt)
                .icon(icon)
                .anchor(0.5f, 0.5f)
                .rotation(213f)
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
    }

    override fun onStart() {
        WebSocketManager.addListener(this)
        super.onStart()
    }

    override fun onStop() {
        WebSocketManager.removeListener(this)
        super.onStop()
    }

    override fun onOpen() {
    }

    override fun onNewLocation(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)

        requireActivity().runOnUiThread {
            vehicleMarker?.position = latLng
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    override fun onClosed() {
    }

    override fun onFailure(t: Throwable) {
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
package com.justjdupuis.tesla_summonpro

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.justjdupuis.tesla_summonpro.databinding.FragmentFirstBinding



/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var map: GoogleMap

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        var lastLatInput: Double = 48.8566
        var lastLonInput: Double = 2.3522
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFrag = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)

        /*val latInput = binding.latitudeInput
        val lonInput = binding.longitudeInput

        binding.buttonFirst.setOnClickListener {
            //findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)

            val lat = latInput.text.toString().toDoubleOrNull()
            val lon = lonInput.text.toString().toDoubleOrNull()

            if (lat != null && lon != null) {
                lastLatInput = lat;
                lastLonInput = lon;
            }

            Toast.makeText(requireContext(), "Lat: $lastLatInput, Lon: $lastLonInput", Toast.LENGTH_SHORT).show()
        }*/

    }

    fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // move camera somewhere:
        val defaultLoc = LatLng(45.5017, -73.5673)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f))

        val orig = BitmapFactory.decodeResource(resources, R.drawable.vehicle_mapmarker_online)

        val size = dpToPx(48)
        val smallBmp = Bitmap.createScaledBitmap(orig, size, size, false)

        val icon = BitmapDescriptorFactory.fromBitmap(smallBmp)
        val pt = LatLng(45.402120, -73.479887)
        map.addMarker(
            MarkerOptions()
                .position(pt)
                .icon(icon)
                .anchor(0.5f, 0.5f)
                .rotation(45f)
                .flat(true)
         )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
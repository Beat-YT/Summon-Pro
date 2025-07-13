package com.justjdupuis.tesla_summonpro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
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

        val latInput = binding.latitudeInput
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
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // move camera somewhere:
        val defaultLoc = LatLng(45.5017, -73.5673)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
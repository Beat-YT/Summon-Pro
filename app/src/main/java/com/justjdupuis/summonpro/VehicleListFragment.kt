package com.justjdupuis.summonpro

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.justjdupuis.summonpro.api.TelemetryApi
import com.justjdupuis.summonpro.api.TeslaApi
import com.justjdupuis.summonpro.utils.TokenStore
import kotlinx.coroutines.launch

class VehicleListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: VehicleRecyclerViewAdapter
    private val vehicleList = mutableListOf<TeslaApi.Vehicle>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_vehicle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.list)
        progressBar = view.findViewById(R.id.progress_spinner)

        adapter = VehicleRecyclerViewAdapter(vehicleList, ::onVehicleClicked)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        loadVehicles()
    }

    private fun loadVehicles() {
        val token = TokenStore.getAccessToken(requireContext())
        if (token == null) {
            // Handle missing token: show error, redirect, or retry login
            Toast.makeText(requireContext(), "No access token found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val vehicleResponse = TeslaApi.service.getVehicleList(token)

                vehicleList.clear()
                vehicleList.addAll(vehicleResponse.response)

                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load vehicles", Toast.LENGTH_SHORT).show()
                Log.e("VehicleListFragment", "API error", e)
            }
        }
    }

    private fun onVehicleClicked(vehicle: TeslaApi.Vehicle) {
        val token = TokenStore.getAccessToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "No access token found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val telemetryState = TelemetryApi.service.getTelemetryInfo(token, vehicle.vin)
                Log.d("VehicleListFragment", "onVehicleClicked ${vehicle.vin}: keyPaired=${telemetryState.keyPaired} limitReached=${telemetryState.limitReached}")
                if (telemetryState.limitReached) {
                    showAlert(
                        "Telemetry Limit Reached",
                        "This vehicle has reached the maximum number of telemetry configurations allowed by Tesla (3 per vehicle). You cannot add new streaming access at this time.\n" +
                                "\n" +
                                "Please remove an existing telemetry app from your Tesla before trying again."
                    )
                    return@launch
                }

                val carState = TeslaApi.service.getVehicleInfo(token, vehicle.vin)
                if (carState.response.state == "offline") {
                    showAlert(
                        "Car is Asleep",
                        "Please wake your car using the Tesla app first.\n\nJust tap the car, or open the Summon tab. Any of these will wake it up."
                    )
                    return@launch
                }

                val bundle = bundleOf(
                    "vin" to vehicle.vin,
                    "displayName" to vehicle.displayName
                )

                if (telemetryState.keyPaired) {
                    findNavController().navigate(R.id.action_VehicleListFragment_to_FirstFragment, bundle)
                } else {
                    findNavController().navigate(R.id.action_VehicleListFragment_to_VirtualKeyIntroFragment, bundle)
                }
            } catch (e: Exception) {
                Log.e("VehicleListFragment", "Failed to fetch telemetry info", e)
                Toast.makeText(requireContext(), "Failed to load telemetry", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

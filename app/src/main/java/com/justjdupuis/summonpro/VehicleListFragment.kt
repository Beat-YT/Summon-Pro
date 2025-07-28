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
import androidx.navigation.navOptions
import com.justjdupuis.summonpro.api.AuthApi
import com.justjdupuis.summonpro.api.TelemetryApi
import com.justjdupuis.summonpro.api.TeslaApi
import com.justjdupuis.summonpro.utils.TokenStore
import kotlinx.coroutines.launch
import retrofit2.HttpException

class VehicleListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: VehicleRecyclerViewAdapter
    private val vehicleList = mutableListOf<TeslaApi.Vehicle>()
    private var isLoggedOut = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_vehicle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.list)
        progressBar = view.findViewById(R.id.progress_spinner)

        setupRecyclerView()
        loadVehicles()
    }

    override fun onResume() {
        super.onResume()
        if (isLoggedOut) {
            replaceScreenLogin()
        }
    }

    private fun setupRecyclerView() {
        adapter = VehicleRecyclerViewAdapter(vehicleList, ::onVehicleClicked)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadVehicles() {
        showLoading()

        lifecycleScope.launch {
            val token = TokenManager.getValidAccessToken(requireContext()) ?: run {
                replaceScreenLogin()
                return@launch
            }

            try {
                val response = TeslaApi.service.getVehicleList(token)
                vehicleList.clear()
                vehicleList.addAll(response.response)
                adapter.notifyDataSetChanged()
                hideLoading()
            } catch (e: Exception) {
                handleApiError(e)
                Log.e("VehicleListFragment", "loadVehicles() Failed", e)
                showError("Failed to load vehicles")
            }
        }
    }

    private fun onVehicleClicked(vehicle: TeslaApi.Vehicle) {
        val token = TokenStore.getAccessToken(requireContext())
        if (token == null) {
            showError("No access token found")
            return
        }

        lifecycleScope.launch {
            try {
                val telemetry = TelemetryApi.service.getTelemetryInfo(token, vehicle.vin)
                if (telemetry.limitReached) {
                    showAlert(
                        "Telemetry Limit Reached",
                        "This vehicle has reached the maximum number of telemetry configurations allowed by Tesla (3 per vehicle). You cannot add new streaming access at this time.\n\nPlease remove an existing telemetry app from your Tesla before trying again."
                    )
                    return@launch
                }

                val car = TeslaApi.service.getVehicleInfo(token, vehicle.vin)
                if (car.response.state != "online") {
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

                val destination = if (telemetry.keyPaired) {
                    R.id.action_VehicleListFragment_to_FirstFragment
                } else {
                    R.id.action_VehicleListFragment_to_VirtualKeyIntroFragment
                }

                findNavController().navigate(destination, bundle)
            } catch (e: Exception) {
                handleApiError(e)
                Log.e("VehicleListFragment", "Failed to handle vehicle click", e)
                showError("Failed to load vehicle")
            }
        }
    }

    private fun handleApiError(e: Exception) {
        if (e is HttpException && e.code() == 401) {
            Log.w("Auth", "401: Unauthorized – likely invalid or expired token")
            isLoggedOut = true
            TokenStore.clear(requireContext())
            replaceScreenLogin()
        }
    }

    private fun replaceScreenLogin() {
        findNavController().navigate(
            R.id.WelcomeFragment,
            null,
            navOptions {
                popUpTo(R.id.nav_graph) {
                    inclusive = true
                }
            }
        )
    }
    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showError(message: String) {
        hideLoading()
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}


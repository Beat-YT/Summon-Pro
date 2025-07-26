package com.justjdupuis.summonpro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.justjdupuis.summonpro.api.TelemetryApi
import com.justjdupuis.summonpro.databinding.FragmentVirtualKeyIntroBinding
import com.justjdupuis.summonpro.utils.Carpenter
import com.justjdupuis.summonpro.utils.TokenStore
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [VirtualKeyIntroFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VirtualKeyIntroFragment : Fragment() {
    private var _binding: FragmentVirtualKeyIntroBinding? = null
    private val binding get() = _binding!!

    private var firstResume = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentVirtualKeyIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if (firstResume) {
            firstResume = false
            return // ignore the first time
        }

        val vin = arguments?.getString("vin")
        if (vin == null) {
            Toast.makeText(requireContext(), "Error VIN not provided", Toast.LENGTH_SHORT).show()
            return
        }
        val displayName = arguments?.getString("displayName") ?: Carpenter.decodeTeslaVin(vin)
        validateTeslaKeyPair(vin, displayName)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val vin = arguments?.getString("vin")
        if (vin == null) {
            Toast.makeText(requireContext(), "Error VIN not provided", Toast.LENGTH_SHORT).show()
            return
        }

        val displayName = arguments?.getString("displayName") ?: Carpenter.decodeTeslaVin(vin)

        binding.teslaVkeyLearnMore.setOnClickListener {
            val url = "https://developer.tesla.com/docs/fleet-api/virtual-keys/overview"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
        
        binding.btnContinue.setOnClickListener {
            val url = "https://www.tesla.com/_ak/summon-pro.cc"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.teslamotors.tesla")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                showAlert(
                    "Unable to open tesla app",
                    "Make sure the tesla app is installed on this phone and paired to the car."
                )
            }
        }

        binding.iAddedKeyAct.setOnClickListener {
            validateTeslaKeyPair(vin, displayName)
        }
    }

    private fun validateTeslaKeyPair(vin: String, name: String) {
        val token = TokenStore.getAccessToken(requireContext())
        if (token == null) {
            Toast.makeText(requireContext(), "No access token found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val telemetryState = TelemetryApi.service.getTelemetryInfo(token, vin)
                if (telemetryState.limitReached) {
                    showAlert(
                        "Telemetry Limit Reached",
                        "This vehicle has reached the maximum number of telemetry configurations allowed by Tesla (3 per vehicle). You cannot add new streaming access at this time.\n" +
                                "\n" +
                                "Please remove an existing telemetry app from your Tesla before trying again."
                    )
                    return@launch
                }

                if (!telemetryState.keyPaired) {
                    showAlert(
                        "Key Not Present",
                        "The virtual key doesn’t seem to be added.\n\nMake sure you selected the vehicle \"$name\" when pairing. The key must be linked to the same car you picked in SummonPro. If you're unsure, try re-pairing."
                    )
                    return@launch
                }

                val bundle = bundleOf( "vin" to vin)
                findNavController().navigate(R.id.action_VirtualKeyIntroFragment_to_FirstFragment, bundle)
            }  catch (e: Exception) {
                Log.e("VirtualKeyIntroFragment", "Failed to fetch telemetry info", e)
                Toast.makeText(requireContext(), "Failed to check for virtual key status", Toast.LENGTH_SHORT).show()
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
package com.justjdupuis.summonpro

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.justjdupuis.summonpro.databinding.FragmentWelcomeBinding
import java.util.UUID

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener { view ->
            val authUrl = Uri.parse("https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/authorize").buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", "43b9cc42-e27a-4da7-a7e2-6e93b814dd3b")
                .appendQueryParameter("redirect_uri", "com.justjdupuis.summonpro://login/tesla-auth")
                .appendQueryParameter("scope", "openid offline_access vehicle_location vehicle_device_data")
                .appendQueryParameter("state", UUID.randomUUID().toString())
                .appendQueryParameter("require_requested_scopes", "true")
                .appendQueryParameter("show_keypair_step", "true")
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .build()

            customTabsIntent.launchUrl(requireContext(), authUrl)
        }
    }
}
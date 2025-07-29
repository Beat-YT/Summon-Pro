package com.justjdupuis.summonpro

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.justjdupuis.summonpro.databinding.FragmentWelcomeBinding
import com.justjdupuis.summonpro.utils.TokenStore
import kotlinx.coroutines.launch
import java.util.UUID

class WelcomeFragment : Fragment() {
    companion object {
        private const val CLIENT_ID = "43b9cc42-e27a-4da7-a7e2-6e93b814dd3b"
        private const val REDIRECT_URI = "com.justjdupuis.summonpro://login/tesla-auth"
        private const val SCOPES = "openid offline_access vehicle_location vehicle_device_data"
    }

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
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("scope", SCOPES)
                .appendQueryParameter("state", UUID.randomUUID().toString())
                .appendQueryParameter("require_requested_scopes", "true")
                .appendQueryParameter("show_keypair_step", "true")
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .build()

            customTabsIntent.launchUrl(requireContext(), authUrl)
        }

        val fullText = "By continuing, you agree to the Terms and Conditions"
        val spannable = SpannableString(fullText)

        val termsStart = fullText.indexOf("Terms and Conditions")
        val termsEnd = termsStart + "Terms and Conditions".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = "https://summon-pro.cc/terms"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                widget.context.startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // underline
                ds.color = ContextCompat.getColor(requireContext(), R.color.textSecondary) // or any color
            }
        }

        spannable.setSpan(clickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.termsText.text = spannable
        binding.termsText.movementMethod = LinkMovementMethod.getInstance()
        binding.termsText.highlightColor = Color.TRANSPARENT

        binding.termsText.setOnClickListener {
            val url = "https://summon-pro.cc/terms"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            val token = TokenManager.getValidAccessToken(requireContext())
            if (token != null) {
                findNavController().navigate(
                    R.id.VehicleListFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.nav_graph) {
                            inclusive = true
                        }
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
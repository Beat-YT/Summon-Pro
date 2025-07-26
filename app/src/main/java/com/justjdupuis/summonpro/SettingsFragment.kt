package com.justjdupuis.summonpro

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val context = requireContext()

        val mockPref = findPreference<ListPreference>("mock_mode")
        val claimPref = findPreference<ListPreference>("claim_distance")
        val aboutPref = findPreference<Preference>("about")
        val logoutPref = findPreference<Preference>("logout")

        fun updateMockSummary(value: String?) {
            val summary = when (value) {
                "global" -> "Clamp to 6m — EU-style limit"
                "china_na" -> "Clamp to 85m — NA & China geofence"
                else -> "Unknown mode"
            }
            mockPref?.summary = summary
        }

        fun updateClaimSummary(value: String?) {
            claimPref?.summary = "$value m before switching to next point"
        }

        updateMockSummary(mockPref?.value)
        updateClaimSummary(claimPref?.value)

        mockPref?.setOnPreferenceChangeListener { _, newValue ->
            updateMockSummary(newValue as String)
            true
        }

        claimPref?.setOnPreferenceChangeListener { _, newValue ->
            updateClaimSummary(newValue as String)
            true
        }

        aboutPref?.setOnPreferenceClickListener {
            AlertDialog.Builder(context)
                .setTitle("About SummonPro")
                .setMessage(
                    """
                    SummonPro was built because this is how Smart Summon should be.
                    
                    Not limited by 85 meters. Not tied to short-range Bluetooth. Just… better.
                    
                    Developed by a Canadian Model 3 owner with HW3, who got tired of seeing “Move Closer” every time he walked away from his car.
                    
                    SummonPro helps your car find you — wherever you are.
                    """.trimIndent()
                )
                .setPositiveButton("OK", null)
                .show()
            true
        }

        logoutPref?.setOnPreferenceClickListener {
            AlertDialog.Builder(context)
                .setTitle("Unpair?")
                .setMessage("This will erase your stored credentials. To remove the key from your Tesla, use the Tesla app.")
                .setPositiveButton("Unpair") { _, _ ->
                    /*val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    prefs.edit().clear().apply()
                    Toast.makeText(context, "Unpaired.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.LoginFragment)*/
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }
}

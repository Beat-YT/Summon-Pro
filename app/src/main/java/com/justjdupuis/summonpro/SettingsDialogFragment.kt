package com.justjdupuis.summonpro

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class SettingsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragment = SettingsFragment()
        childFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commitNowAllowingStateLoss()

        val dialog = Dialog(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)
        dialog.setContentView(R.layout.dialog_fragment_container) // We'll define this XML below
        return dialog
    }
}
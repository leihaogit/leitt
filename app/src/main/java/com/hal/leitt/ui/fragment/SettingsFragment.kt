package com.hal.leitt.ui.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.hal.leitt.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

}
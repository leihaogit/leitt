package com.hal.leitt.ui.fragment

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.hal.leitt.R
import com.hal.leitt.ktx.Constant
import com.tencent.mmkv.MMKV

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 偏好设置页面
 */

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var packageManager: PackageManager
    private lateinit var inflater: LayoutInflater
    private lateinit var winManager: WindowManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        packageManager = requireContext().packageManager
        inflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        winManager = requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager

        //初始化偏好设置
        initPreferences()

    }

    private fun initPreferences() {
        findPreference<SwitchPreferenceCompat>("function")?.setOnPreferenceChangeListener { _, newValue ->
            val isChecked = newValue as Boolean
            if (isChecked) {
                // 开关被打开
                MMKV.defaultMMKV().encode(Constant.IS_FUNCTION_ON, true)
            } else {
                // 开关被关闭
                MMKV.defaultMMKV().encode(Constant.IS_FUNCTION_ON, false)
            }
            true
        }

    }
}

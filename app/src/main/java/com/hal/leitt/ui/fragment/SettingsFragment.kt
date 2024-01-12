package com.hal.leitt.ui.fragment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.hal.leitt.R
import com.hal.leitt.entity.AppInfo
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

    override fun onResume() {
        super.onResume()
        //检测到服务异常终止则直接关闭该界面
        if (!MMKV.defaultMMKV().decodeBool(Constant.IS_ACC_RUNNING)) {
            Toast.makeText(
                requireContext(), resources.getString(R.string.acc_unbind), Toast.LENGTH_SHORT
            ).show()
            requireActivity().finish()
        }
    }

    private fun initPreferences() {
        /**
         * 基础功能开关
         */
        val function: SwitchPreferenceCompat? = findPreference("function")
        function?.let {

            //初始化
            it.isChecked = MMKV.defaultMMKV().decodeBool(Constant.IS_FUNCTION_ON, false)

            it.setOnPreferenceChangeListener { _, newValue ->
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

        /**
         * 广告检测时长设置
         */
        val duration: SeekBarPreference? = findPreference("duration")
        duration?.let {

            //初始化
            it.value = MMKV.defaultMMKV().decodeInt(Constant.AD_DETECTION_DURATION, 4)

            it.setOnPreferenceChangeListener { _, newValue ->
                MMKV.defaultMMKV().encode(Constant.AD_DETECTION_DURATION, newValue as Int)
                true
            }

        }

        /**
         * 关键字
         */
        val keyword: EditTextPreference? = findPreference("keyword")
        keyword?.let {

            //初始化
            it.text = MMKV.defaultMMKV().decodeString(Constant.KEYWORD, "跳过")

            it.setOnPreferenceChangeListener { _, newValue ->
                MMKV.defaultMMKV().encode(Constant.KEYWORD, (newValue as String).trim())
                true
            }

        }

        /**
         * 白名单
         */
        val whiteList: Preference? = findPreference("whitelist")
        whiteList?.setOnPreferenceClickListener {

            //查询设备上能够响应主活动启动意图的应用程序，获取所有应用程序的包名，并保存在appList列表中
            val appList: MutableList<String> = ArrayList()
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfoList =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (e in resolveInfoList) {
                appList.add(e.activityInfo.packageName)
            }

            //app信息名单
            val appInfoList: MutableList<AppInfo> = ArrayList()
            //应用白名单包名集合
            val pkgWhitelist: Set<String> = getWhitelistPackages() ?: emptySet()
            //遍历所有应用
            for (pkgName in appList) {

            }


            true
        }


    }

    /**
     * 获取白名单应用，返回包名的集合
     */
    private fun getWhitelistPackages(): Set<String>? {
        return null
    }
}

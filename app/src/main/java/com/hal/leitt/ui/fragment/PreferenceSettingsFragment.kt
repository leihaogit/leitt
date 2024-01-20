package com.hal.leitt.ui.fragment

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hal.leitt.R
import com.hal.leitt.adapter.AppInfoAdapter
import com.hal.leitt.entity.AppInfo
import com.hal.leitt.entity.PackageWidgetDescription
import com.hal.leitt.ktx.PreferenceSettings
import com.hal.leitt.service.SkipAdService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 偏好设置页面
 */

class PreferenceSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var packageManager: PackageManager
    private lateinit var inflater: LayoutInflater

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        packageManager = requireContext().packageManager
        inflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    private lateinit var manageWidgets: MultiSelectListPreference

    //包名及对应控件信息映射
    private var mapPackageWidgets: MutableMap<String, MutableSet<PackageWidgetDescription>> =
        mutableMapOf()

    override fun onResume() {
        super.onResume()
        //初始化偏好设置
        initPreferences()
    }

    fun initPreferences() {
        /**
         * 基础功能开关
         */
        val function: SwitchPreferenceCompat = findPreference("function")!!
        function.let {

            //初始化，意外中止可能会导致 MMKV 更新不了字段，所以额外判断一下
            if (!SkipAdService.isServiceRunning()) {
                it.isChecked = false
            } else it.isChecked = PreferenceSettings.isFunctionOn()

            it.setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue as Boolean
                if (isChecked) {
                    //服务未开启不允许开启基础功能
                    if (!SkipAdService.isServiceRunning()) {
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.pls_bind_acc),
                            Toast.LENGTH_SHORT
                        ).show()
                        //阻止开关状态变化
                        return@setOnPreferenceChangeListener false
                    } else {
                        // 开关被打开
                        PreferenceSettings.setFunctionOn(true)
                    }
                } else {
                    // 开关被关闭
                    PreferenceSettings.setFunctionOn(false)
                }
                true
            }

        }

        /**
         * 关键字
         */
        val keyword: EditTextPreference = findPreference("keyword")!!
        keyword.let {

            //初始化
            it.text = PreferenceSettings.getKeyWords().joinToString(" ")

            it.setOnPreferenceChangeListener { _, newValue ->
                PreferenceSettings.setKeyWords((newValue as String).trim())
                // 刷新关键字列表
                SkipAdService.dispatchAction(SkipAdService.ACTION_REFRESH_KEYWORDS)
                true
            }

        }

        /**
         * 白名单
         */
        val whiteList: Preference = findPreference("whitelist")!!
        whiteList.setOnPreferenceClickListener {
            //限制只显示一个框
            it.isEnabled = false
            val progressDialog = ProgressDialog(requireContext())
            progressDialog.setMessage(resources.getString(R.string.loading_app_list))
            progressDialog.setCancelable(false)
            progressDialog.show()

            // 启动协程加载应用列表
            lifecycleScope.launch {
                val appInfoList = getAppInfoList()
                showAppInfoListDialog(it, appInfoList)
                progressDialog.dismiss()
            }
            true
        }


        /**
         * 采集按钮
         */
        val gatherButtons: Preference = findPreference("gather_buttons")!!
        gatherButtons.setOnPreferenceClickListener {
            if (!SkipAdService.dispatchAction(SkipAdService.ACTION_ACTIVITY_CUSTOMIZATION)) {
                Toast.makeText(
                    context, resources.getString(R.string.pls_bind_acc), Toast.LENGTH_SHORT
                ).show()
            }
            true
        }

        /**
         *  管理已经采集按钮的应用
         */
        manageWidgets = findPreference("manage_widgets")!!
        mapPackageWidgets = PreferenceSettings.getMapPackageWidgets()
        Log.e("halo", "已采集控件信息: $mapPackageWidgets")
        updateMultiSelectListPreferenceEntries(manageWidgets, mapPackageWidgets.keys)
        manageWidgets.setOnPreferenceChangeListener { _, newValue ->
            val results = newValue as MutableSet<*>
            val keys: MutableSet<String> = mapPackageWidgets.keys.toMutableSet()
            for (key in keys) {
                if (!results.contains(key)) {
                    mapPackageWidgets.remove(key)
                }
            }
            PreferenceSettings.setMapPackageWidgets(mapPackageWidgets)
            updateMultiSelectListPreferenceEntries(manageWidgets, mapPackageWidgets.keys)
            SkipAdService.dispatchAction(SkipAdService.ACTION_REFRESH_CUSTOMIZED_ACTIVITY)
            true
        }

        /**
         * 管理规则
         */
        val manageRules: Preference = findPreference("manage_rules")!!
        manageRules.setOnPreferenceClickListener {
            val fragmentManager = requireActivity().supportFragmentManager
            val newFragment = ManagePackageWidgetsDialogFragment()
            newFragment.show(fragmentManager, "dialog")
            true
        }

    }

    /**
     * 更新偏好设置中某一个MultiSelectListPreference的内容
     */
    private fun updateMultiSelectListPreferenceEntries(
        preference: MultiSelectListPreference, keys: MutableSet<String>
    ) {
        val entries = keys.toTypedArray<CharSequence>()
        preference.entries = entries
        preference.entryValues = entries
        preference.values = keys
    }

    /**
     * 展示白名单列表
     */
    private fun showAppInfoListDialog(preference: Preference, appInfoList: List<AppInfo>) {
        // 弹窗的视图
        val dialogLayout: View = inflater.inflate(R.layout.dialog_whitelist_layout, null, false)
        val appInfoAdapter = AppInfoAdapter()
        val rvWhitelist = dialogLayout.findViewById<RecyclerView>(R.id.rv_whitelist)
        rvWhitelist.layoutManager = LinearLayoutManager(requireContext())
        rvWhitelist.adapter = appInfoAdapter
        appInfoAdapter.submitList(appInfoList)

        val dialog: AlertDialog =
            AlertDialog.Builder(requireContext()).setView(dialogLayout).create()
        dialog.show()
        dialog.setOnDismissListener {
            preference.isEnabled = true
        }

        val btnCancel = dialogLayout.findViewById<Button>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        val btnConfirm = dialogLayout.findViewById<Button>(R.id.btn_confirm)
        btnConfirm.setOnClickListener {
            // 保存更新后的白名单应用
            val pkgWhitelistNew: MutableSet<String> = HashSet()
            for (app in appInfoList) {
                if (app.isChecked) {
                    pkgWhitelistNew.add(app.packageName)
                }
            }
            PreferenceSettings.setWhiteList(pkgWhitelistNew)

            // 刷新包信息
            SkipAdService.dispatchAction(SkipAdService.ACTION_REFRESH_PACKAGE)

            dialog.dismiss()
        }
    }

    /**
     * 获取所有应用列表，耗时，使用协程来做
     */
    private suspend fun getAppInfoList(): List<AppInfo> = withContext(Dispatchers.IO) {
        //查询设备上能够响应主活动启动意图的应用程序，获取所有应用程序的包名，并保存在appList列表中
        val appList: MutableList<String> = ArrayList()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            appList.add(e.activityInfo.packageName)
        }
        //app信息名单
        val appInfoList: MutableList<AppInfo> = ArrayList()
        //应用白名单包名集合
        val pkgWhitelist = PreferenceSettings.getWhiteList()
        //遍历所有应用
        for (pkgName in appList) {
            val info = packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            val applicationName = packageManager.getApplicationLabel(info).toString()
            //默认不是白名单
            val appInfo = AppInfo(
                pkgName, applicationName, packageManager.getApplicationIcon(info), false
            )
            //检查是否是白名单应用
            appInfo.isChecked = pkgWhitelist.contains(pkgName)
            appInfoList.add(appInfo)
        }
        //排序 规则为 白名单 + 首字母 降序
        appInfoList.sort()
        return@withContext appInfoList
    }

}

package com.hal.leitt.ui.fragment

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hal.leitt.R
import com.hal.leitt.adapter.AppInfoAdapter
import com.hal.leitt.entity.AppInfo
import com.hal.leitt.ktx.Settings
import com.hal.leitt.service.TouchHelperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat

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

    }

    override fun onResume() {
        super.onResume()
        //初始化偏好设置
        initPreferences()
    }

    private fun initPreferences() {
        /**
         * 基础功能开关
         */
        val function: SwitchPreferenceCompat? = findPreference("function")
        function?.let {

            //初始化
            it.isChecked = Settings.isFunctionOn()

            it.setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue as Boolean
                if (isChecked) {
                    //服务未开启不允许开启基础功能
                    if (!TouchHelperService.isServiceRunning()) {
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.pls_bind_acc),
                            Toast.LENGTH_SHORT
                        ).show()
                        //阻止开关状态变化
                        return@setOnPreferenceChangeListener false
                    } else {
                        // 开关被打开
                        Settings.setFunctionOn(true)
                    }
                } else {
                    // 开关被关闭
                    Settings.setFunctionOn(false)
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
            it.value = Settings.getAdDetectionDuration()

            it.setOnPreferenceChangeListener { _, newValue ->
                Settings.setAdDetectionDuration(newValue as Int)
                true
            }

        }

        /**
         * 关键字
         */
        val keyword: EditTextPreference? = findPreference("keyword")
        keyword?.let {

            //初始化
            it.text = Settings.getKeyWords().joinToString(" ")

            it.setOnPreferenceChangeListener { _, newValue ->
                Settings.setKeyWords((newValue as String).trim())
                // 刷新关键字列表
                TouchHelperService.dispatchAction(TouchHelperService.ACTION_REFRESH_KEYWORDS)
                true
            }

        }

        /**
         * 白名单
         */
        val whiteList: Preference? = findPreference("whitelist")
        whiteList?.setOnPreferenceClickListener {
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
        val activityCustomization: Preference? = findPreference("setting_activity_customization")
        activityCustomization?.setOnPreferenceClickListener {
            if (!TouchHelperService.dispatchAction(TouchHelperService.ACTION_ACTIVITY_CUSTOMIZATION)) {
                Toast.makeText(
                    context, resources.getString(R.string.pls_bind_acc), Toast.LENGTH_SHORT
                ).show()
            }
            true
        }

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
            Settings.setWhiteList(pkgWhitelistNew)

            // 刷新包信息
            TouchHelperService.dispatchAction(TouchHelperService.ACTION_REFRESH_PACKAGE)

            dialog.dismiss()
        }
    }

    /**
     * 获取白名单应用，有点耗时，开个协程来做
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
        val pkgWhitelist: MutableSet<String> = Settings.getWhiteList()
        //遍历所有应用
        for (pkgName in appList) {
            val info = packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            val applicationName = packageManager.getApplicationLabel(info).toString()
            //中文 - 拼音
            val applicationNamePinYin = try {
                PinyinHelper.toHanYuPinyinString(
                    applicationName, HanyuPinyinOutputFormat(), "", true
                )
            } catch (e: Exception) {
                applicationName
            }
            //默认不是白名单
            val appInfo = AppInfo(
                pkgName,
                applicationName,
                applicationNamePinYin,
                packageManager.getApplicationIcon(info),
                false
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

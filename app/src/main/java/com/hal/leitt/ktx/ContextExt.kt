package com.hal.leitt.ktx

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.hal.leitt.App
import com.hal.leitt.entity.AppInfo
import com.tencent.mmkv.MMKV


/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description Context 的扩展函数
 */

/**
 * 遍历本机所有应用（除系统 + 自身）
 */
@Suppress("DEPRECATION")
fun Context.getInstalledAppList(pattern: String = ""): MutableList<AppInfo> {
    // 创建一个 Intent，用于查询所有启动的应用程序
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    // 使用 queryIntentActivities 获取所有匹配的应用列表
    val appInfoList = packageManager.queryIntentActivities(intent, 0)
        .filterNot { isSystemApp(it) || isSelf(it) || !isContains(it, pattern) }.map {
            val appName = it.loadLabel(packageManager).toString()
            val packageName = it.activityInfo.packageName
            val icon = it.activityInfo.loadIcon(packageManager)
            // 是否检查广告，默认为 true
            val isCheckAd = MMKV.defaultMMKV().decodeBool("$packageName.isCheckAd", true)
            AppInfo(
                appName, packageName, icon, isCheckAd
            )
        }.toMutableList()
    return appInfoList
}

// 检查应用是否为系统应用
private fun isSystemApp(resolveInfo: ResolveInfo): Boolean {
    val regex = "com\\.android\\.[^.]+"
    val packageName = resolveInfo.activityInfo.packageName
    return packageName.matches(Regex(regex))
}

// 检查应用是否为自身
private fun isSelf(resolveInfo: ResolveInfo): Boolean {
    return resolveInfo.activityInfo.packageName == App.appContext.packageName
}

// 检查应用是否包含指定字符串
private fun isContains(resolveInfo: ResolveInfo, pattern: String): Boolean {
    val appName = resolveInfo.loadLabel(App.appContext.packageManager).toString()
    return appName.contains(pattern)
}
package com.hal.leitt.ktx

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hal.leitt.App
import com.hal.leitt.entity.PackageWidgetDescription
import com.tencent.mmkv.MMKV

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 用于封装本地配置的保存和读取
 */

object Settings {

    /**
     * 获取关键字列表，默认"跳过"
     */
    fun getKeyWords(): MutableList<String> {
        return MMKV.defaultMMKV().decodeString(Constant.KEYWORD, "跳过")!!
            .split(" ") as MutableList<String>
    }

    /**
     * 保存关键字
     */
    fun setKeyWords(keyWords: String) {
        MMKV.defaultMMKV().encode(Constant.KEYWORD, keyWords)
    }

    /**
     * 跳广告功能是否开启，默认 false - 不开启
     */
    fun isFunctionOn(): Boolean {
        return MMKV.defaultMMKV().decodeBool(Constant.IS_FUNCTION_ON, false)
    }

    /**
     * 设置跳广告功能的开启状态
     */
    fun setFunctionOn(status: Boolean) {
        MMKV.defaultMMKV().encode(Constant.IS_FUNCTION_ON, status)
    }

    /**
     * 获取广告检测时长，默认4s
     */
    fun getAdDetectionDuration(): Int {
        return MMKV.defaultMMKV().decodeInt(Constant.AD_DETECTION_DURATION, 4)
    }

    /**
     * 设置广告检测时长
     */
    fun setAdDetectionDuration(duration: Int) {
        MMKV.defaultMMKV().encode(Constant.AD_DETECTION_DURATION, duration)
    }

    /**
     * 设置检测应用白名单
     */
    fun setWhiteList(list: MutableSet<String>) {
        MMKV.defaultMMKV().encode(Constant.WHITELIST, list)
    }

    /**
     * 获取检测应用白名单，默认空名单
     */
    fun getWhiteList(): MutableSet<String> {
        //如果是第一次获取，生成一个默认的初始白名单，包含系统应用以及自身
        val packageManager: PackageManager = App.appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val pkgSystems: MutableSet<String> = HashSet()
        for (e in resolveInfoList) {
            if (e.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM) {
                pkgSystems.add(e.activityInfo.packageName)
            }
        }
        pkgSystems.add(App.appContext.packageName)
        return MMKV.defaultMMKV().decodeStringSet(Constant.WHITELIST, pkgSystems)!!
    }

    /**
     * 获取控件信息映射
     */
    fun getMapPackageWidgets(): MutableMap<String, MutableSet<PackageWidgetDescription>> {
        val jsonString = MMKV.defaultMMKV().decodeString(Constant.PACKAGE_WIDGETS, "{}")
        val type =
            object : TypeToken<MutableMap<String, MutableSet<PackageWidgetDescription>>>() {}.type
        return Gson().fromJson(jsonString, type)
    }

    /**
     * 保存控件信息映射
     */
    fun setMapPackageWidgets(mapPackageWidgets: MutableMap<String, MutableSet<PackageWidgetDescription>>) {
        val jsonString = Gson().toJson(mapPackageWidgets)
        MMKV.defaultMMKV().encode(Constant.PACKAGE_WIDGETS, jsonString)
    }

    /**
     * 保存控件信息映射（自定义规则），解析并保存成功返回true，否则返回false
     */
    fun setMapPackageWidgetsInString(rules: String): Boolean {
        return try {
            val type = object :
                TypeToken<MutableMap<String, MutableSet<PackageWidgetDescription>>>() {}.type
            Gson().fromJson(rules, type) as MutableMap<String, MutableSet<PackageWidgetDescription>>
            MMKV.defaultMMKV().encode(Constant.PACKAGE_WIDGETS, rules)
            true
        } catch (e: Exception) {
            false
        }
    }
}
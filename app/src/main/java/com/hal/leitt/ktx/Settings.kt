package com.hal.leitt.ktx

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hal.leitt.entity.PackagePositionDescription
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
        return MMKV.defaultMMKV().decodeStringSet(Constant.WHITELIST, mutableSetOf())!!
    }

    /**
     * 获取包控件信息映射
     */
    fun getMapPackageWidgets(): MutableMap<String, MutableSet<PackageWidgetDescription>> {
        val jsonString = MMKV.defaultMMKV().decodeString(Constant.PACKAGE_WIDGETS, "{}")
        val gson = Gson()
        val type =
            object : TypeToken<MutableMap<String, MutableSet<PackageWidgetDescription>>>() {}.type
        return gson.fromJson(jsonString, type)
    }

    /**
     * 保存包控件信息映射
     */
    fun setMapPackageWidgets(mapPackageWidgets: MutableMap<String, MutableSet<PackageWidgetDescription>>) {
        val gson = Gson()
        val jsonString = gson.toJson(mapPackageWidgets)
        MMKV.defaultMMKV().encode(Constant.PACKAGE_WIDGETS, jsonString)
    }

    /**
     * 保存包位置信息映射
     */
    fun setMapPackagePositions(mapPackagePositions: MutableMap<String, PackagePositionDescription>) {
        val gson = Gson()
        val jsonString = gson.toJson(mapPackagePositions)
        MMKV.defaultMMKV().encode(Constant.PACKAGE_POSITIONS, jsonString)
    }

    /**
     * 获取包位置信息映射
     */
    fun getMapPackagePositions(): MutableMap<String, PackagePositionDescription> {
        val jsonString = MMKV.defaultMMKV().decodeString(Constant.PACKAGE_POSITIONS, "{}")
        val gson = Gson()
        val type = object : TypeToken<MutableMap<String, PackagePositionDescription>>() {}.type
        return gson.fromJson(jsonString, type)
    }


}
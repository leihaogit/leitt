package com.hal.leitt.ktx

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
    fun getKeyWords(): List<String> {
        return MMKV.defaultMMKV().decodeString(Constant.KEYWORD, "跳过")!!.split(" ")
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
    fun setWhiteList(list: Set<String>) {
        MMKV.defaultMMKV().encode(Constant.WHITELIST, list)
    }

    /**
     * 获取检测应用白名单，默认空名单
     */
    fun getWhiteList(): MutableSet<String> {
        return MMKV.defaultMMKV().decodeStringSet(Constant.WHITELIST, mutableSetOf())!!
    }

}
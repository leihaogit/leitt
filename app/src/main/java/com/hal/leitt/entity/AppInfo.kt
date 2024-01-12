package com.hal.leitt.entity

import android.graphics.drawable.Drawable

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description app 信息实体类
 */

data class AppInfo(
    val packageName: String,//包名
    val applicationName: String,//应用名
    val applicationNamePinYin: String,//中文拼音名 方便排序
    val applicationIcon: Drawable,//icon
    var isChecked: Boolean//是否是白名单应用
) : Comparable<AppInfo> {
    override fun compareTo(other: AppInfo): Int {
        // 先根据 isChecked 进行排序，isChecked 为 true 的应用排在前面
        if (isChecked && !other.isChecked) {
            return -1
        }
        if (!isChecked && other.isChecked) {
            return 1
        }

        // 如果都是 isChecked 或者都不是 isChecked，则按照应用名的汉字或英文首字母顺序进行排序
        return applicationNamePinYin.compareTo(other.applicationNamePinYin, ignoreCase = true)
    }
}

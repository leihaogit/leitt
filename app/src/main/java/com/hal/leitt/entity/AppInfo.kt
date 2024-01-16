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
    val applicationIcon: Drawable,//icon
    var isChecked: Boolean//是否是白名单应用
) : Comparable<AppInfo> {
    override fun compareTo(other: AppInfo): Int {
        if (isChecked && !other.isChecked) {
            return -1
        } else if (!isChecked && other.isChecked) {
            return 1
        }
        return 0
    }
}

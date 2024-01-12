package com.hal.leitt.entity

import android.graphics.drawable.Drawable

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 每一个本机应用的信息
 */

data class AppInfo(
    val appName: String,//app名称
    val packageName: String,//包名
    val appIcon: Drawable,//icon资源
    var isCheckAd: Boolean,//是否检测广告，默认为true
)
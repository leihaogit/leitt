package com.hal.leitt.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.view.accessibility.AccessibilityEvent


/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 自动触控服务具体实现
 */

class TouchHelperServiceImpl(private val service: AccessibilityService) {

    private var currentPackageName: String? = null
    private var currentActivityName: String? = null
    var receiverHandler: Handler? = null

    /**
     * 建立连接，开始初始化工作
     */
    fun onServiceConnected() {

        currentPackageName = "Initial PackageName"
        currentActivityName = "Initial ClassName"

    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val tempPkgName = event.packageName
        val tempClassName = event.className
        if (tempPkgName == null || tempClassName == null) return
    }

    fun onUnbind() {
        try {

        } catch (_: Throwable) {
        }
    }

}
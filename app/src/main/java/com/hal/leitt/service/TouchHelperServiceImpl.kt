package com.hal.leitt.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.hal.leitt.ktx.Settings
import com.hal.leitt.receiver.PackageChangeReceiver
import com.hal.leitt.receiver.UserPresentReceiver


/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 自动触控服务具体实现
 */

class TouchHelperServiceImpl(private val service: AccessibilityService) {

    //处理广播信息的Handler
    var receiverHandler: Handler? = null

    //关键字列表
    private var keyWordList: List<String> = mutableListOf()

    //白名单包名列表 - 需排除
    private var whiteList: MutableSet<String> = mutableSetOf()

    //输入法包名列表 - 需排除
    private var setIMEApps: MutableSet<String> = mutableSetOf()

    //待检测的包名列表 - 需检测
    private var setPackages: MutableSet<String> = mutableSetOf()

    //包变化广播接收器
    private val packageChangeReceiver by lazy {
        PackageChangeReceiver()
    }

    //用户开启屏幕广播接收器
    private val userPresentReceiver by lazy { UserPresentReceiver() }


    /**
     * 建立连接，开始初始化工作
     */
    fun onServiceConnected() {

        Log.e("halo", "无障碍服务已启动")

        //初始化关键字列表

        keyWordList = Settings.getKeyWords()

        //初始化白名单列表
        whiteList = Settings.getWhiteList()
        updatePackage()

        // 初始化接收器
        installReceiverAndHandler()

    }

    private fun installReceiverAndHandler() {

        //广播注册
        service.registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        Log.e("halo", "用户屏幕开启广播已注册")
        val actions = IntentFilter()
        actions.addAction(Intent.ACTION_PACKAGE_ADDED)
        actions.addAction(Intent.ACTION_PACKAGE_REMOVED)
        service.registerReceiver(packageChangeReceiver, actions)
        Log.e("halo", "包变化广播已注册")

        // 处理广播事件
        receiverHandler = Handler(Looper.getMainLooper()) { msg: Message ->
            when (msg.what) {
                //关键字更新
                TouchHelperService.ACTION_REFRESH_KEYWORDS -> {
                    Log.e("halo", "关键字刷新: $keyWordList")
                    keyWordList = Settings.getKeyWords()
                }
                //包状态更新
                TouchHelperService.ACTION_REFRESH_PACKAGE -> {
                    Log.e("halo", "白名单刷新: $whiteList")
                    whiteList = Settings.getWhiteList()
                    updatePackage()
                }

                TouchHelperService.ACTION_REFRESH_CUSTOMIZED_ACTIVITY -> {

                }
                //打开采集按钮弹窗
                TouchHelperService.ACTION_ACTIVITY_CUSTOMIZATION -> {

                }

                TouchHelperService.ACTION_START_SKIP_AD -> {
                    Log.e("halo", "启动跳广告进程")
                }

                TouchHelperService.ACTION_STOP_SKIP_AD -> {
                    Log.e("halo", "关闭跳广告进程")
                }
            }
            true
        }
    }

    /**
     * 刷新包状态，在启动时查找所有包。也会在接收包添加/删除事件时触发
     */
    private fun updatePackage() {

    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val tempPkgName = event.packageName
        val tempClassName = event.className
        if (tempPkgName == null || tempClassName == null) return
    }

    fun onUnbind() {
        service.unregisterReceiver(userPresentReceiver)
        Log.e("halo", "用户屏幕开启广播已注销")
        service.unregisterReceiver(packageChangeReceiver)
        Log.e("halo", "包变化广播已注销")
        Log.e("halo", "无障碍服务已关闭")
    }

}
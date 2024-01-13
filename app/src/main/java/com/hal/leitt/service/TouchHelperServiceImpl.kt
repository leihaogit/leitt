package com.hal.leitt.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
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

    //包管理器
    private val packageManager: PackageManager = service.packageManager


    /**
     * 建立连接，开始初始化工作
     */
    fun onServiceConnected() {

        Log.e("halo", "==========无障碍服务已启动==========")

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
        actions.addDataScheme("package")
        service.registerReceiver(packageChangeReceiver, actions)
        Log.e("halo", "系统包变化广播已注册")

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
     * 刷新要检测的包列表状态，在启动时查找所有包。也会在系统安装/卸载应用时触发
     */
    private fun updatePackage() {
        Log.e("halo", "==========开始过滤包列表=========")
        //临时包列表，里面存放一些需要排除的包名
        val setTemps: MutableSet<String> = mutableSetOf()

        var resolveInfoList: List<ResolveInfo>
        //查询设备上所有的启动器（Launcher）应用程序，并放入 setPackages 中
        var intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            setPackages.add(e.activityInfo.packageName)
        }
        Log.e("halo", "待过滤时，setPackages的大小: " + setPackages.size)

        //查询设备上所有的主屏幕（Home）应用程序，，并放入 setTemps 中
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            setTemps.add(e.activityInfo.packageName)
        }
        Log.e("halo", "主屏幕过滤后，setTemps的大小: " + setTemps.size)

        //查询设备上所有的输入法（imm）应用程序，并放入 setIMEApps 中
        val inputMethodInfoList =
            (service.getSystemService(AccessibilityService.INPUT_METHOD_SERVICE) as InputMethodManager).inputMethodList
        for (e in inputMethodInfoList) {
            setIMEApps.add(e.packageName)
        }
        Log.e("halo", "输入法过滤后，setIMEApps的大小: " + setIMEApps.size)

        //将当前应用程序和系统设置应用程序加入临时过滤集合
        setTemps.add(service.packageName)
        setTemps.add("com.android.settings")
        Log.e("halo", "添加自身和设置之后，setTemps的大小: " + setTemps.size)

        //移除白名单+输入法+临时列表
        setPackages.removeAll(whiteList)
        setPackages.removeAll(setIMEApps)
        setPackages.removeAll(setTemps)

        Log.e("halo", "移除白名单+输入法+临时列表之后，setPackages 最终的大小：" + setPackages.size)

        Log.e("halo", "==========结束过滤包列表=========")

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
        Log.e("halo", "系统包变化广播已注销")
        Log.e("halo", "==========无障碍服务已关闭==========")
    }

}
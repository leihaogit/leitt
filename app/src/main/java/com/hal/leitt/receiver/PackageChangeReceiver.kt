package com.hal.leitt.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hal.leitt.service.TouchHelperService

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 当系统安装或删除软件时的广播
 */

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_PACKAGE_ADDED || intent?.action == Intent.ACTION_PACKAGE_REMOVED) {
            //表示用户安装或卸载新的应用时的广播
            TouchHelperService.dispatchAction(TouchHelperService.ACTION_REFRESH_PACKAGE)
        }
    }
}
package com.hal.leitt.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hal.leitt.service.TouchHelperService

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 当屏幕开启时的广播
 */

class UserPresentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            //唤醒无障碍服务
            TouchHelperService.dispatchAction(TouchHelperService.ACTION_WAKE_UP)
        }
    }
}
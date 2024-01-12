package com.hal.leitt.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hal.leitt.service.TouchHelperService


/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 屏幕由关闭转为开启状态
 */

class UserPresentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            //表示用户解锁设备并且屏幕变为可见状态时的广播
            TouchHelperService.dispatchAction(TouchHelperService.ACTION_START_SKIP_AD)
        }
    }
}
package com.hal.leitt.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference


/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 自动触控服务基础 ACC
 */

class TouchHelperService : AccessibilityService() {

    companion object {
        const val ACTION_REFRESH_KEYWORDS = 1
        const val ACTION_REFRESH_PACKAGE = 2
        const val ACTION_REFRESH_CUSTOMIZED_ACTIVITY = 3
        const val ACTION_ACTIVITY_CUSTOMIZATION = 4
        const val ACTION_STOP_SERVICE = 5
        const val ACTION_START_SKIP_AD = 6
        const val ACTION_STOP_SKIP_AD = 7

        private var mService: WeakReference<TouchHelperService>? = null

        fun dispatchAction(action: Int): Boolean {
            val service = mService?.get()
            return if (service?.serviceImpl != null) {
                service.serviceImpl?.receiverHandler?.sendEmptyMessage(action)
                true
            } else {
                false
            }
        }

        fun isServiceRunning(): Boolean {
            val service = mService?.get()
            return service?.serviceImpl != null
        }
    }

    private var serviceImpl: TouchHelperServiceImpl? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        mService = WeakReference(this)
        if (serviceImpl == null) {
            serviceImpl = TouchHelperServiceImpl(this)
        }
        serviceImpl?.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        serviceImpl?.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
    }

    override fun onUnbind(intent: Intent): Boolean {
        serviceImpl?.onUnbind()
        serviceImpl = null
        mService = null
        return super.onUnbind(intent)
    }

}
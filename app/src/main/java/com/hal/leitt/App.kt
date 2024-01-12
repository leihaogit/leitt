package com.hal.leitt

import android.app.Application
import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * ...
 * @author LeiHao
 * @date 2024/1/4
 * @description APP
 */

class App : Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        MMKV.initialize(this)
    }
}
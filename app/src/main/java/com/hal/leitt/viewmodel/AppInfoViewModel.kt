package com.hal.leitt.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hal.leitt.App
import com.hal.leitt.entity.AppInfo
import com.hal.leitt.ktx.getInstalledAppList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ...
 * @author LeiHao
 * @date 2024/1/5
 * @description ViewModel
 */

class AppInfoViewModel : ViewModel() {
    private var appInfoList: MutableList<AppInfo>? = null
        set(value) {
            field = value
            appInfoListLiveData.postValue(value)
        }

    var appInfoListLiveData = MutableLiveData<MutableList<AppInfo>>()

    /**
     * 获取目前本机安卓的应用
     * @param pattern app名需要包含的字符串
     */
    fun getInstalledAppInfoList(pattern: String): MutableList<AppInfo>? {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appInfoList = App.appContext.getInstalledAppList(pattern)
            }
        }
        return appInfoList
    }

}
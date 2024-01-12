package com.hal.leitt.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


/**
 * ...
 * @author LeiHao
 * @date 2024/1/11
 * @description 主页viewModel
 */

class HomeViewModel : ViewModel() {
    //无障碍权限状态
    val mAccessibilityPermission: MutableLiveData<Boolean> = MutableLiveData()

    //电源优化权限状态
    val mPowerOptimization: MutableLiveData<Boolean> = MutableLiveData()
}
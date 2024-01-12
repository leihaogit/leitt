package com.hal.leitt.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hal.leitt.R
import com.hal.leitt.databinding.ActivityHomeBinding
import com.hal.leitt.ktx.viewBinding
import com.hal.leitt.service.TouchHelperService
import com.hal.leitt.viewmodel.HomeViewModel

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 主页
 */

class HomeActivity : AppCompatActivity() {

    private val binding: ActivityHomeBinding by viewBinding()

    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.title = resources.getString(R.string.app_name)
        context = this

        initData()

        initEvent()

    }

    private fun initEvent() {

        //开启无障碍服务
        binding.clGotoAcc.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        //前往设置电池优化
        binding.clGotoPower.setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

    }

    private fun initData() {

        //观察无障碍权限状态
        homeViewModel.mAccessibilityPermission.observe(this) {
            binding.ivAccStatus.isSelected = it
            if (it) {
                binding.tvGotoAcc.text = resources.getString(R.string.already_setting)
                binding.tvGotoAccArrow.isVisible = false
                binding.tvGotoAcc.setTextColor(
                    ContextCompat.getColor(
                        context, R.color.custom_color
                    )
                )
            } else {
                binding.tvGotoAcc.text = resources.getString(R.string.go_now)
                binding.tvGotoAccArrow.isVisible = true
                binding.tvGotoAcc.setTextColor(ContextCompat.getColor(context, R.color.red))
                binding.tvGotoAccArrow.setTextColor(ContextCompat.getColor(context, R.color.red))
            }
        }

        //观察电源优化权限状态
        homeViewModel.mPowerOptimization.observe(this) {
            binding.ivPowerStatus.isSelected = it
            if (it) {
                binding.tvGotoPower.text = resources.getString(R.string.already_setting)
                binding.tvGotoPowerArrow.isVisible = false
                binding.tvGotoPower.setTextColor(
                    ContextCompat.getColor(
                        context, R.color.custom_color
                    )
                )
            } else {
                binding.tvGotoPower.text = resources.getString(R.string.go_now)
                binding.tvGotoPowerArrow.isVisible = true
                binding.tvGotoPower.setTextColor(ContextCompat.getColor(context, R.color.red))
                binding.tvGotoPowerArrow.setTextColor(ContextCompat.getColor(context, R.color.red))
            }
        }

    }

    override fun onResume() {
        super.onResume()
        checkServiceStatus()
    }

    /**
     * 检查服务状态
     */
    private fun checkServiceStatus() {
        homeViewModel.mAccessibilityPermission.value = TouchHelperService.isServiceRunning()
        val pm = context.getSystemService(POWER_SERVICE) as PowerManager
        homeViewModel.mPowerOptimization.value =
            pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    //处理actionBar菜单栏
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        val item = menu.findItem(R.id.app_bar_setting)
        item.setOnMenuItemClickListener {
            item.isEnabled = false
            Handler(mainLooper).postDelayed({ item.isEnabled = true }, 500)
            if (!binding.ivAccStatus.isSelected) {
                SettingsActivity.start(this)
            } else {
                Toast.makeText(
                    context, resources.getString(R.string.pls_bind_acc), Toast.LENGTH_SHORT
                ).show()
            }
            true
        }
        return true
    }

}

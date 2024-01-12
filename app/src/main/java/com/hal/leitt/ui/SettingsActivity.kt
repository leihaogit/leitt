package com.hal.leitt.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.hal.leitt.R
import com.hal.leitt.databinding.ActivitySettingsBinding
import com.hal.leitt.ktx.viewBinding
import com.hal.leitt.ui.fragment.SettingsFragment

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 设置页
 */

class SettingsActivity : AppCompatActivity() {

    private val binding: ActivitySettingsBinding by viewBinding()

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.title = resources.getString(R.string.settings)
        supportFragmentManager.beginTransaction().replace(R.id.frameLayout, SettingsFragment())
            .commit()
    }

    //处理actionBar菜单栏
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        val item = menu.findItem(R.id.app_bar_share)
        item.setOnMenuItemClickListener {
            true
        }
        return true
    }
}
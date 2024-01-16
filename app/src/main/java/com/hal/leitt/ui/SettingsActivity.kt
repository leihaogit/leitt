package com.hal.leitt.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.hal.leitt.R
import com.hal.leitt.databinding.ActivitySettingsBinding
import com.hal.leitt.ktx.viewBinding
import com.hal.leitt.ui.fragment.SettingsFragment
import java.io.File

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 设置页
 */

class SettingsActivity : AppCompatActivity() {

    private val binding: ActivitySettingsBinding by viewBinding()

    private lateinit var context: Context

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        context = this

        supportActionBar?.title = resources.getString(R.string.settings)

        initData()


    }

    private fun initData() {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
        //放置 Fragment
        supportFragmentManager.beginTransaction().replace(R.id.frameLayout, SettingsFragment())
            .commit()
    }

    //处理actionBar菜单栏
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        val item = menu.findItem(R.id.app_bar_share)
        item.setOnMenuItemClickListener {
            item.isEnabled = false
            Handler(mainLooper).postDelayed({ item.isEnabled = true }, 500)
            val apkFile = File(context.applicationContext.packageResourcePath)
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(apkFile))
            context.startActivity(intent)
            true
        }
        return true
    }
}
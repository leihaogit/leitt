package com.hal.leitt.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hal.leitt.R
import com.hal.leitt.adapter.AppInfoAdapter
import com.hal.leitt.databinding.ActivityAppConfigBinding
import com.hal.leitt.databinding.DialogAppConfigBinding
import com.hal.leitt.ktx.viewBinding
import com.hal.leitt.viewmodel.AppInfoViewModel
import com.tencent.mmkv.MMKV

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description app配置界面
 */

class AppConfigActivity : AppCompatActivity() {

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, AppConfigActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private val appInfoViewModel: AppInfoViewModel by viewModels()

    private val appInfoAdapter by lazy {
        AppInfoAdapter()
    }

    private val binding: ActivityAppConfigBinding by viewBinding()

    private var mPattern = ""

    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.title = resources.getString(R.string.app_list)
        context = this

        initData()

    }

    override fun onResume() {
        super.onResume()
        //获取一下已安装的app信息
        appInfoViewModel.getInstalledAppInfoList(mPattern)
    }

    private fun initData() {
        binding.rvAppInfo.layoutManager = LinearLayoutManager(context)
        binding.rvAppInfo.adapter = appInfoAdapter
        appInfoViewModel.appInfoListLiveData.observe(this) {
            appInfoAdapter.submitList(it)
        }
        appInfoAdapter.onItemClick = { appInfo ->
            val view = layoutInflater.inflate(R.layout.dialog_app_config, null)
            val binding = DialogAppConfigBinding.bind(view)
            binding.cbCheckAd.isChecked = appInfo.isCheckAd
            binding.cbCheckAd.setOnCheckedChangeListener { _, isChecked ->
                appInfo.isCheckAd = isChecked
            }
            AlertDialog.Builder(context).setView(view).setTitle(appInfo.appName)
                .setIcon(appInfo.appIcon)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
                .setPositiveButton(resources.getString(R.string.confirm)) { _, _ ->
                    MMKV.defaultMMKV()
                        .encode("${appInfo.packageName}.isCheckAd", binding.cbCheckAd.isChecked)
                    //刷新一下
                    appInfoViewModel.getInstalledAppInfoList(mPattern)
                }.show()
        }
    }

    //处理actionBar菜单栏
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_config_menu, menu)
        val searchView: SearchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.maxWidth = resources.displayMetrics.widthPixels / 2

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String): Boolean {
                val pattern = p0.trim()
                //获取一下已安装的app信息，过滤信息为输入的信息
                mPattern = pattern
                appInfoViewModel.getInstalledAppInfoList(mPattern)
                return true
            }
        })

        return true
    }


}
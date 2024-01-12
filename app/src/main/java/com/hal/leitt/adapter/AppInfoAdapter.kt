package com.hal.leitt.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.hal.leitt.databinding.RecycleItemWhitelistBinding
import com.hal.leitt.entity.AppInfo

/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 白名单应用适配器
 */

class AppInfoAdapter : ListAdapter<AppInfo, AppInfoAdapter.MyViewHolder>(AppInfoDiffCallBack()) {

    class AppInfoDiffCallBack : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }

    class MyViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecycleItemWhitelistBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val appInfo = getItem(position)
        val binding = holder.binding as RecycleItemWhitelistBinding
        binding.check.isChecked = appInfo.isChecked
        binding.icon.setImageDrawable(appInfo.applicationIcon)
        binding.name.text = appInfo.applicationName
        binding.root.setOnClickListener {
            appInfo.isChecked = !appInfo.isChecked
            binding.check.isChecked = appInfo.isChecked
        }
    }
}
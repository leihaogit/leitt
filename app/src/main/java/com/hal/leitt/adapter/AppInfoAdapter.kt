package com.hal.leitt.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.hal.leitt.R
import com.hal.leitt.databinding.RecycleItemAppInfoBinding
import com.hal.leitt.entity.AppInfo


/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 应用列表适配器
 */

class AppInfoAdapter : ListAdapter<AppInfo, AppInfoAdapter.MyViewHolder>(DiffCallBack()) {

    class DiffCallBack : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }

    var onItemClick: ((AppInfo) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            RecycleItemAppInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val appInfo = getItem(position)
        when (val binding = holder.binding) {
            is RecycleItemAppInfoBinding -> {
                binding.tvAppName.text = appInfo.appName
                binding.tvPackageName.text = appInfo.packageName
                binding.ivIcon.setImageDrawable(appInfo.appIcon)
                binding.tvCheckAd.text =
                    if (appInfo.isCheckAd) binding.root.context.resources.getString(
                        R.string.apple_green
                    ) else binding.root.context.resources.getString(
                        R.string.apple_red
                    )
                binding.root.setOnClickListener {
                    onItemClick?.invoke(
                        appInfo ?: return@setOnClickListener
                    )
                }
            }
        }
    }

    class MyViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
}
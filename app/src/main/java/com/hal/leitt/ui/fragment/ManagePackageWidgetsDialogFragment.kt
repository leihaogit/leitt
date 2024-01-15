package com.hal.leitt.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import com.hal.leitt.databinding.LayoutManagePackageWidgetsBinding
import com.hal.leitt.ktx.Settings
import com.hal.leitt.ktx.viewBinding
import com.hal.leitt.ui.SettingsActivity

/**
 * ...
 * @author LeiHao
 * @date 2024/1/15
 * @description 手动管理规则的Fragment
 */

class ManagePackageWidgetsDialogFragment : DialogFragment() {
    private val binding: LayoutManagePackageWidgetsBinding by viewBinding()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etRules.setText(Gson().toJson(Settings.getMapPackageWidgets()))
        //复制
        binding.btnCopy.setOnClickListener {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("规则", binding.etRules.text.toString())
            clipboard.setPrimaryClip(clip)
        }

        //粘贴
        binding.btnPaste.setOnClickListener {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasteData = clipData.getItemAt(0).text.toString()
                binding.etRules.setText(pasteData)
            }
        }

        //保存
        binding.btnConfirm.setOnClickListener {
            if (Settings.setMapPackageWidgetsInString(binding.etRules.text.toString())) {
                dismiss()
                Toast.makeText(requireContext(), "规则已保存", Toast.LENGTH_SHORT).show()
                (requireActivity() as SettingsActivity).supportFragmentManager.fragments.forEach {
                    if (it is SettingsFragment) {
                        it.refresh()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "规则格式不正确", Toast.LENGTH_SHORT).show()
            }
        }

        //取消
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        //清空
        binding.btnClear.setOnClickListener {
            binding.etRules.setText("{}")
        }

    }


}
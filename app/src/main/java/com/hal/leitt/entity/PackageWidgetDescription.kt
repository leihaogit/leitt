package com.hal.leitt.entity

import android.graphics.Rect
import java.io.Serializable

/**
 * ...
 * @author LeiHao
 * @date 2024/1/13
 * @description 描述应用程序窗体信息和特征
 */
data class PackageWidgetDescription(
    var packageName: String,//包名
    var activityName: String,//activity包名
    var className: String,//类名
    var idName: String,//ID名称
    var description: String,//描述
    var text: String,//文本内容
    var position: Rect,//坐标位置
    var clickable: Boolean,//是否可点击
    var onlyClick: Boolean//是否只可以点击
) : Serializable {
    constructor(packageWidgetDescription: PackageWidgetDescription) : this(
        "", "", "", "", "", "", Rect(), clickable = false, onlyClick = false
    ) {
        this.packageName = packageWidgetDescription.packageName
        this.activityName = packageWidgetDescription.activityName
        this.className = packageWidgetDescription.className
        this.idName = packageWidgetDescription.idName
        this.description = packageWidgetDescription.description
        this.text = packageWidgetDescription.text
        this.position = packageWidgetDescription.position
        this.clickable = packageWidgetDescription.clickable
        this.onlyClick = packageWidgetDescription.onlyClick
    }
}
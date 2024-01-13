package com.hal.leitt.entity

import java.io.Serializable


/**
 * ...
 * @author LeiHao
 * @date 2024/1/13
 * @description 包信息描述
 */

data class PackagePositionDescription(
    var packageName: String,//包名
    var activityName: String,//activity名
    var x: Int,//横坐标
    var y: Int//纵坐标
) : Serializable {
    constructor(packagePositionDescription: PackagePositionDescription) : this(
        "", "", 0, 0
    ) {
        this.packageName = packagePositionDescription.packageName
        this.activityName = packagePositionDescription.activityName
        this.x = packagePositionDescription.x
        this.y = packagePositionDescription.y
    }
}
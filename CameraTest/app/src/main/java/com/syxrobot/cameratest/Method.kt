package com.syxrobot.cameratest

import android.content.Context
import android.content.pm.PackageManager

/**
 *  @author:JinXuDong
 *  @date:2018/7/3
 */


/**
 * 检测相机是否可用
 */
fun checkCameraHardware(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
}
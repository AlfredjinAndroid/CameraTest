# Android Camera 简单封装

## 思路

1、按照官方的API，简单封装个可行的CameraView

2、能够简单的设置部分相机配置

3、由于需要，所以添加原始帧数据的预览获取

4、优化代码，更简单的调用

## 实现

### CameraView代码
先来展示代码，之后再进行说明:

```
package com.syxrobot.cameratest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.ByteArrayOutputStream

@SuppressLint("ViewConstructor")
/**
 *  @author:JinXuDong
 *  @date:2018/7/3
 */
class CameraView(context: Context?) : SurfaceView(context), SurfaceHolder.Callback, Camera.PreviewCallback {

    private var mCamera: Camera? = null
    private var mHolder: SurfaceHolder = holder
    private var mListener: ((ByteArray, Camera) -> Unit)? = null

    init {
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (mHolder.surface == null) {
            return
        }
        mCamera?.stopPreview()
        mCamera?.setPreviewCallback(this)
        mCamera?.setPreviewDisplay(mHolder)
        mCamera?.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mCamera?.stopPreview()
        releaseCamera()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        mCamera?.setPreviewCallback(this)
        mCamera?.setPreviewDisplay(holder)
        mCamera?.startPreview()
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        if (mListener != null) {
            mListener?.invoke(data, camera)
        }

    }


    /**
     * 预览数据
     * 数据为原始数据，需要拍摄图片时需要自己转换格式
     * 或可以调用其他方法转换 如 [onPreviewJpeg] 或 [onPreviewNV21ToJpeg]
     */
    fun onPreview(l: ((data: ByteArray, camera: Camera) -> Unit)) {
        this.mListener = l
    }

    /**
     * 预览数据
     * NV21 转 JPEG
     * @param width 宽
     * @param height 高
     * @param quality 图片质量
     * @param l 回调函数  调用者需要在此做自己的操作
     */
    fun onPreviewNV21ToJpeg(width: Int, height: Int, quality: Int, l: ((data: ByteArray) -> Unit)) {
        onPreviewJpeg(ImageFormat.NV21, width, height, quality, l)
    }

    /**
     * 预览数据转换成JPEG格式
     */
    fun onPreviewJpeg(format: Int, width: Int, height: Int, quality: Int, l: ((data: ByteArray) -> Unit)) {
        onPreview { data, _ ->
            val yuv = YuvImage(data, format, width, height, null)
            val ops = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, width, height), quality, ops)
            l.invoke(ops.toByteArray())
        }
    }


    /**
     * 释放Camera资源
     * 释放之前 可以判断是否已经被释放 [isReleased]
     */
    fun releaseCamera() {
        if (mCamera != null) {
            synchronized(mCamera!!) {
                mCamera?.setPreviewCallback(null)
                mCamera?.stopPreview()
                mCamera?.release()
                mCamera = null
            }
        }
    }

    fun isReleased(): Boolean = mCamera == null

    /**
     * 获取Camera
     */
    fun openCamera(cameraId: Int): CameraView {
        mCamera = Camera.open(cameraId)
        return this
    }

    /**
     * 相机参数设置
     */
    fun cameraConfig(cameraParameters: CameraParameters) {
        cameraParameters.commit()
    }

    /**
     * 返回相机
     */
    fun getCamera(): Camera? {
        return mCamera
    }

    /**
     *  @author:JinXuDong
     *  @date:2018/7/3
     *  相机参数
     */
    inner class CameraParameters {
        private val mParameters: Camera.Parameters? = mCamera?.parameters
        fun setPreViewSize(width: Int, height: Int): CameraParameters {
            mParameters?.setPreviewSize(width, height)
            return this
        }

        /**
         *  设置预览图片图像格式。
         *  @param format 预览格式
         */
        fun setPreviewFormat(format: Int): CameraParameters {
            mParameters?.previewFormat = format
            return this
        }

        /**
         * 设置白平衡
         */
        fun setWhiteBalance(value: String): CameraParameters {
            mParameters?.whiteBalance = value
            return this
        }

        /**
         * 设置场景模式
         */
        fun setSceneMode(value: String): CameraParameters {
            mParameters?.sceneMode = value
            return this
        }

        /**
         * 设置曝光补偿指数。
         * @param value 曝光值
         */
        fun setExposureCompensation(value: Int): CameraParameters {
            mParameters?.exposureCompensation = value
            return this
        }

        /**
         * 设置相机旋转角度
         */
        fun setDisplayOrientation(degrees: Int): CameraParameters {
            mCamera?.setDisplayOrientation(degrees)
            return this
        }

        /**
         * 提交设置
         */
        fun commit() {
            mCamera?.parameters = mParameters
        }
    }
}
```

### CameraView代码说明
首先继承自SurfaceView，因为我们需要将预览的数据展示到SurfaceView上。

所以要实现以下三个方法
surfaceChanged()
surfaceDestroyed()
surfaceCreated()

我们在SurfaceView被创建时设置预览，在被销毁时同时释放相机资源，在状态改变时先停止预览，再重新预览。

**openCamera(cameraId: Int)**   
用来打开相机,cameraId 代表前置或后置相机  
0 后置 1 前置  
可以直接调用CameraInfo的前置或后置摄像机的常量

**cameraConfig()**  
设置相机参数

**onPreview(l: ((data: ByteArray, camera: Camera) -> Unit))**  
获取预览数据(原始数据)

**onPreviewJpeg(format: Int, width: Int, height: Int, quality: Int, l: ((data: ByteArray) -> Unit))**  
获取预览数据(转换后的数据，JPEG)

**onPreviewNV21ToJpeg(width: Int, height: Int, quality: Int, l: ((data: ByteArray) -> Unit))**  
获取预览数据(原始数据为NV21，转换后为JPEG)

**releaseCamera()**  
释放相机资源

**isReleased()**  
相机资源是否被释放

**getCamera()**  
获取相机实例，可以做更多自己的操作  

### CameraParameters 代码说明

**setPreViewSize(width: Int, height: Int)**  
设置预览分辨率大小
  
**setPreviewFormat(format: Int)**  
设置预览图片图像格式。 

**setWhiteBalance(value: String)**  
设置白平衡

**setSceneMode(value: String)**  
设置场景模式

**setExposureCompensation(value: Int)**  
设置曝光补偿指数

**setDisplayOrientation(degrees: Int)**  
设置相机旋转角度

**commit()**
提交设置 

## 使用方法

activity_main.xml布局文件
```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <FrameLayout
        android:id="@+id/mFC"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <Button
        android:id="@+id/mBtn"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="拍照"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent" />
</android.support.constraint.ConstraintLayout>
```

```kotlin
package com.syxrobot.cameratest

import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var cameraPreview: CameraView
    private var isTakePic: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkCameraHardware(this)) {
            /**
             * 开启相机预览页
             */
            cameraPreview = CameraView(this)
                    .openCamera(Camera.CameraInfo.CAMERA_FACING_BACK)
            /**
             * 相机参数设置
             */
            cameraPreview.cameraConfig(cameraPreview.CameraParameters()
                    .setPreViewSize(1280, 720)
                    .setPreviewFormat(ImageFormat.NV21)
                    .setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO)
                    .setSceneMode(Camera.Parameters.SCENE_MODE_AUTO)
                    .setExposureCompensation(3)
            )

            cameraPreview.onPreviewNV21ToJpeg(1280, 720, 70) {
                if (isTakePic) {
                    isTakePic = false
                    val file = File("/storage/emulated/0/test.jpg")
                    file.writeBytes(it)
                    Log.e("TestCamera", "保存成功")
                }
            }
            /**
             * 添加相机预览
             */
            mFC.addView(cameraPreview)
        } else {
            Log.e("TestCamera", "摄像机不能用")
        }

        mBtn.setOnClickListener {
            isTakePic = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!cameraPreview.isReleased()) {
            cameraPreview.releaseCamera()
        }
    }
}

/**
 * 检测相机是否可用
 */
fun checkCameraHardware(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
}

```

### 代码说明

首先开启相机  

然后设置相机参数

最后处理相机预览(如果有需要的话)

再退出程序后，为了避免会出问题，所以谨慎点可以再onDestroy()时手动调用一次释放资源。


## 注意

**0、别忘了权限**
```
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

**1、由于本人开发环境需要兼容API19，为避免Camera2不适配，所以使用的是Camera1**

**2、由于本人的开发环境不需要动态申请权限，所以没写动态申请权限，需要的可以自己写**

**3、为避免由于硬件问题产生的错误，可以使用检测相机的那个方法，那是我从官方API中复制来的  。。。O(∩_∩)O哈哈~**

**4、部分功能还没实现，若问为什么，本宝是个菜鸟，还在努力学习...**
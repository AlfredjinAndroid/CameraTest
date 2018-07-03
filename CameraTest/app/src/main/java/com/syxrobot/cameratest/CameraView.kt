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
        /**
         * 预览分辨率大小
         */
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
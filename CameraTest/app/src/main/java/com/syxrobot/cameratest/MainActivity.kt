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

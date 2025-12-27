package com.alijafari.brik.block.helpers

import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.alijafari.brik.R

class OverlayManager(val context: Context) {
    fun startOverlay(){

        val layoutParamsType: Int =
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams().apply {
            width = 0
            height = 0
            type = layoutParamsType
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            format = PixelFormat.TRANSLUCENT
        }
        params.gravity = Gravity.START

        val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.overlay_view, null)

        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(view, params)
    }
}
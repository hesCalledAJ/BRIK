package com.alijafari.brik.utils

import android.text.TextUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


fun isMiUi(): Boolean {
    return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"))
}

fun getSystemProperty(propName: String?): String? {
    var line: String?
    var input: BufferedReader? = null
    try {
        val p = Runtime.getRuntime().exec("getprop " + propName)
        input = BufferedReader(InputStreamReader(p.getInputStream()), 1024)
        line = input.readLine()
        input.close()
    } catch (ex: IOException) {
        return null
    } finally {
        if (input != null) {
            try {
                input.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return line
}
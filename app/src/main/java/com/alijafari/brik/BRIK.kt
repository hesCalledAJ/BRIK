package com.alijafari.brik

import android.app.Application
import com.alijafari.brik.utils.PreferencesRepository

class BRIK : Application() {
    lateinit var preferencesRepository: PreferencesRepository
    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(applicationContext)
    }
}
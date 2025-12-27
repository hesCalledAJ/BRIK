package com.alijafari.brik.main.presentation

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.alijafari.brik.block.framework.BlockService
import com.alijafari.brik.main.viewmodel.MainViewModel
import com.alijafari.brik.ui.theme.BRIKTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private var bound = false
    private var blockService: BlockService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? BlockService.LocalBinder ?: return
            blockService = binder.getService()
            bound = true
            viewModel.bindSessionRepository(blockService!!)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            blockService = null
            viewModel.unbindSessionRepository()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        enableEdgeToEdge()

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        setContent {
            BRIKTheme {
                MainScreen(
                    viewModel
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, BlockService::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions(this)
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
            blockService = null
            viewModel.unbindSessionRepository()
        }
    }


    override fun onPause() {
        super.onPause()
        if (viewModel.sessionActive.value){
            startActivity(
                Intent(
                    applicationContext, MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.lockNow()
        }
    }
}
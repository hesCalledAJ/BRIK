package com.alijafari.brik.main.presentation

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alijafari.brik.block.framework.AdminManagerReceiver
import com.alijafari.brik.block.framework.BlockService
import com.alijafari.brik.main.viewmodel.MainViewModel
import com.alijafari.brik.ui.theme.BRIKTheme
import com.alijafari.brik.utils.PermissionEvent
import com.alijafari.brik.utils.PermissionType
import kotlinx.coroutines.launch
import androidx.core.net.toUri

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


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.permissionEvent.collect { event ->
                    when (event) {
                        is PermissionEvent.ShowToast ->
                            Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_SHORT).show()

                        is PermissionEvent.LaunchIntent ->
                            handlePermissionIntent(event.type)
                    }
                }
            }
        }
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
        viewModel.refreshPermissions()
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
    private fun handlePermissionIntent(type: PermissionType) {
        val intent = when (type) {
            PermissionType.MIUI_AUTO_START -> {
                try {
                    Intent().apply {
                        component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                    }
                } catch (_: Exception) {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }
            }
            PermissionType.OVERLAY ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())

            PermissionType.DEVICE_ADMIN ->
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this@MainActivity, AdminManagerReceiver::class.java))
                }

            PermissionType.NOTIFICATIONS ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }

            PermissionType.BATTERY_OPTIMIZATION ->
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:$packageName".toUri()
                }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
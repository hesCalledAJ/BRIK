package com.alijafari.brik.block.framework

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alijafari.brik.BRIK
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.e("TAG", "onReceive: Boot received ${context.applicationContext.packageName}", )
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            val app = context.applicationContext as? BRIK ?: return@launch
            val repo = app.preferencesRepository
            val endTime = repo.readLastSessionEndTime().first()
            val now = System.currentTimeMillis()

            if (endTime > now) {
                val remainingSeconds =
                    ((endTime - now) / 1000).toInt()

                Log.e("TAG", "onReceive: $remainingSeconds", )
                val serviceIntent = Intent(context, BlockService::class.java).apply {
                    action = BlockService.INTENT_START
                    putExtra(
                        BlockService.EXTRA_DURATION_SECONDS,
                        remainingSeconds
                    )
                    putExtra(
                        BlockService.EXTRA_FROM_BOOT_RECEIVER,
                        true
                    )
                }

                context.startForegroundService(serviceIntent)
            } else {
                repo.saveSessionEndtime(0L)
            }
        }
    }
}

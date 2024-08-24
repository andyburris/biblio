package com.andb.apps.biblio.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date

@Composable
fun currentTimeAsState(): State<Date> {
    val timeFlow = remember {
        MutableStateFlow(Date())
    }
    LaunchedEffect(key1 = Unit) {
        while(true){
            timeFlow.value = Date()
            delay(1000)
        }
    }
    return timeFlow.collectAsState()
}

data class BatteryState(val percent: Float, val isCharging: Boolean)
@Composable
fun currentBatteryAsState(): State<BatteryState> {
    val batteryState = remember {
        MutableStateFlow(BatteryState(-1f, false))
    }

    val context = LocalContext.current
    val batteryBroadcastReceiver = remember {
        object: BroadcastReceiver() {
            override fun onReceive(context: Context?, batteryStatus: Intent?) {
                Log.d("battery status changed", "intent = ${batteryStatus}")
                val batteryPct: Float? = batteryStatus?.let { intent ->
                    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    Log.d("battery status changed", "level = ${level}, scale = $scale")
                    level / scale.toFloat()
                }
                val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL

                Log.d("battery status changed", "pct = ${batteryPct}, isCharging = $isCharging")
                batteryState.value = BatteryState(batteryPct ?: -1f, isCharging)
            }
        }
    }

    LaunchedEffect(context, batteryBroadcastReceiver) {
        context.registerReceiver(batteryBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    return batteryState.collectAsState()
}
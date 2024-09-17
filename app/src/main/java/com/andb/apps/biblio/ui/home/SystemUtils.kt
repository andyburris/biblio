package com.andb.apps.biblio.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Batterycharging
import com.adamglin.phosphoricons.regular.Batteryempty
import com.adamglin.phosphoricons.regular.Batteryfull
import com.adamglin.phosphoricons.regular.Batteryhigh
import com.adamglin.phosphoricons.regular.Batterylow
import com.adamglin.phosphoricons.regular.Batterymedium
import com.adamglin.phosphoricons.regular.Wifihigh
import com.adamglin.phosphoricons.regular.Wifilow
import com.adamglin.phosphoricons.regular.Wifimedium
import com.adamglin.phosphoricons.regular.Wifinone
import com.adamglin.phosphoricons.regular.Wifislash
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date
import kotlin.math.roundToInt

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

data class BatteryState(val percent: Float?, val isCharging: Boolean, val isSaver: Boolean)
@Composable
fun currentBatteryAsState(): State<BatteryState> {
    val batteryState = remember {
        MutableStateFlow(BatteryState(null, isCharging = false, isSaver = false))
    }

    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val batteryBroadcastReceiver = remember {
        object: BroadcastReceiver() {
            override fun onReceive(context: Context?, batteryStatus: Intent?) {
                val batteryPct: Float? = batteryStatus?.let { intent ->
                    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    level / scale.toFloat()
                }
                val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL
                val isSaver = powerManager.isPowerSaveMode

                batteryState.value = BatteryState(batteryPct ?: -1f, isCharging, isSaver)
            }
        }
    }

    DisposableEffect(context, batteryBroadcastReceiver) {
        context.registerReceiver(batteryBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            context.unregisterReceiver(batteryBroadcastReceiver)
        }
    }

    return batteryState.collectAsState()
}
fun BatteryState.toIcon(): ImageVector = when(this.isCharging) {
    true -> PhosphorIcons.Regular.Batterycharging
    false -> when(this.percent) {
        null -> PhosphorIcons.Regular.Batterycharging
        in 0.85f..1.0f-> PhosphorIcons.Regular.Batteryfull
        in 0.65f..0.85f -> PhosphorIcons.Regular.Batteryhigh
        in 0.4f..0.65f -> PhosphorIcons.Regular.Batterymedium
        in 0.05f..0.4f -> PhosphorIcons.Regular.Batterylow
        else -> PhosphorIcons.Regular.Batteryempty
    }
}
fun BatteryState.toPercentString() = when(this.percent) {
    null -> "..."
    else -> "${(this.percent * 100).roundToInt()}%"
}


sealed class WifiState() {
    data object Off: WifiState()
    data object NoConnection: WifiState()
    data class Connected(val signalStrength: Int, val ssid: String?): WifiState()
}
@Composable
fun wifiSignalAsState(): State<WifiState> {
    val wifiSignal = remember {
        MutableStateFlow<WifiState>(WifiState.NoConnection)
    }

    val context = LocalContext.current
    val connectivityManager = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

    val networkCallback = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    wifiSignal.value = WifiState.Connected(
                        signalStrength = networkCapabilities.signalStrength,
                        ssid = (networkCapabilities.transportInfo as? WifiInfo)?.ssid,
                    )
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    wifiSignal.value = WifiState.NoConnection
                }
            }
            else -> object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    wifiSignal.value = WifiState.Connected(
                        signalStrength = wifiManager.connectionInfo.rssi,
//                        ssid = "${wifiManager.connectionInfo.rssi} dBm",
                        ssid = wifiManager.connectionInfo.ssid?.removeSurrounding("\"")?.let {
                            if(it == WifiManager.UNKNOWN_SSID) null else it
                        },
                    )
                }
                override fun onLost(network: Network) {
                    super.onLost(network)
                    wifiSignal.value = WifiState.NoConnection
                }
            }
        }
    }

    DisposableEffect(context) {
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    return wifiSignal.collectAsState()
}
fun WifiState.toIcon() = when(this) {
    is WifiState.Connected -> when(this.signalStrength) {
        in -30 downTo -40 -> PhosphorIcons.Regular.Wifihigh
        in -40 downTo -55 -> PhosphorIcons.Regular.Wifimedium
        in -55 downTo -70 -> PhosphorIcons.Regular.Wifilow
        else -> PhosphorIcons.Regular.Wifinone
    }
    WifiState.NoConnection -> PhosphorIcons.Regular.Wifinone
    WifiState.Off -> PhosphorIcons.Regular.Wifislash
}
fun WifiState.strengthDescription() = when(this) {
    is WifiState.Connected -> when(this.signalStrength) {
        1 -> "Weak signal"
        2 -> "Medium signal"
        3 -> "Strong signal"
        else -> "No signal"
    }
    WifiState.NoConnection -> "No connection"
    WifiState.Off -> "Wi-Fi off"
}
package com.andb.apps.biblio.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Bookopentext
import com.adamglin.phosphoricons.regular.Gauge
import com.adamglin.phosphoricons.regular.Lightning
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Pushpin
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.Sundim
import com.adamglin.phosphoricons.regular.Sunhorizon
import com.adamglin.phosphoricons.regular.Textaa
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.settings.SettingsPopup
import com.andb.apps.biblio.ui.settings.SettingsPopupHeader
import com.andb.apps.biblio.ui.settings.SettingsPopupItem
import com.andb.apps.biblio.ui.settings.SettingsSegment
import com.andb.apps.biblio.ui.settings.SettingsSegmentItem

@Composable
fun HomeSettingsPopup(
    modifier: Modifier = Modifier,
    onOpenSettingsScreen: () -> Unit,
) {
    val context = LocalContext.current
    val settings = LocalSettings.current

    val batteryState = currentBatteryAsState()
    val wifiState = wifiSignalAsState()

    SettingsPopup(
        modifier = modifier,
    ) {
        item(
            span = { GridItemSpan(this.maxLineSpan) }
        ) {
            SettingsPopupHeader(
                title = "Quick Settings",
                onMore = {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                },
            )
        }
        item { SettingsPopupItem(
            title = "Battery",
            icon = batteryState.value.toIcon(),
            isActivated = false,
            state = batteryState.value.toPercentString()
                    + if(batteryState.value.isCharging) " • Charging" else ""
                    + if(batteryState.value.isSaver) " • Battery Saver" else "",
            onMore = { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)); },
        ) }

        item { SettingsPopupItem(
            title = "Wi-Fi",
            icon = wifiState.value.toIcon(),
            isActivated = wifiState.value is WifiState.Connected,
            state = when(val wifi = wifiState.value) {
                is WifiState.Connected -> when(wifi.ssid) {
                    null -> "Needs permission"
                    else -> wifi.ssid
                }
                is WifiState.NoConnection -> "No connected network"
                is WifiState.Off -> "Off"
            },
            onMore = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)); },
        ) }

        item(
            span = { GridItemSpan(this.maxLineSpan) }
        ) {
            SettingsSegmentItem(
                title = "Brightness",
                segments = listOf(
                    SettingsSegment("night", "Night", PhosphorIcons.Regular.Moon),
                    SettingsSegment("low", "Low", PhosphorIcons.Regular.Sunhorizon),
                    SettingsSegment("medium", "Medium", PhosphorIcons.Regular.Sundim),
                    SettingsSegment("high", "High", PhosphorIcons.Regular.Sun),
                ),
                selectedSegmentKey = "low",
                onSelect = {},
                onMore = {},
            )
        }

        item(span = { GridItemSpan(this.maxLineSpan) }) {
            SettingsSegmentItem(
                title = "Refresh Rate",
                segments = listOf(
                    SettingsSegment("reading", "Reading", PhosphorIcons.Regular.Bookopentext),
                    SettingsSegment("slow", "Slow", PhosphorIcons.Regular.Textaa),
                    SettingsSegment("medium", "Medium", PhosphorIcons.Regular.Lightning),
                    SettingsSegment("fast", "Fast", PhosphorIcons.Regular.Gauge),
                ),
                selectedSegmentKey = "reading",
                onSelect = {},
                onMore = {},
            )
        }

        item(span = { GridItemSpan(this.maxLineSpan) }) {
            SettingsPopupHeader(
                title = "Biblio Settings • Home",
                onMore = { onOpenSettingsScreen()},
            )
        }
        item { SettingsPopupItem(
            title = "Pinned Apps",
            icon = PhosphorIcons.Regular.Pushpin,
            isActivated = null,
            state = when(val pinned = settings.settings.home.pinnedAppsList) {
                emptyList<String>() -> "None"
                else -> pinned.joinToString { it }
            },
            onMore = {},
        ) }
        item { SettingsPopupItem(settingState = settings.common.showNumbers) }
    }
}
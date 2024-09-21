package com.andb.apps.biblio.ui.home

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Arrowright
import com.adamglin.phosphoricons.regular.Batteryfull
import com.adamglin.phosphoricons.regular.Bookopentext
import com.adamglin.phosphoricons.regular.Caretright
import com.adamglin.phosphoricons.regular.Gauge
import com.adamglin.phosphoricons.regular.Lightning
import com.adamglin.phosphoricons.regular.Moon
import com.adamglin.phosphoricons.regular.Percent
import com.adamglin.phosphoricons.regular.Pushpin
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.Sundim
import com.adamglin.phosphoricons.regular.Sunhorizon
import com.adamglin.phosphoricons.regular.Textaa
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.theme.BiblioTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsPopup(
    modifier: Modifier = Modifier,
    onOpenTestScreen: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val batteryState = currentBatteryAsState()
    val wifiState = wifiSignalAsState()

    val settings = LocalSettings.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(196.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color = BiblioTheme.colors.divider, shape = RoundedCornerShape(8.dp))
            .background(BiblioTheme.colors.background, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
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
        item { SettingsItem(
            title = "Battery",
            icon = batteryState.value.toIcon(),
            isActivated = false,
            state = batteryState.value.toPercentString()
                    + if(batteryState.value.isCharging) " • Charging" else ""
                    + if(batteryState.value.isSaver) " • Battery Saver" else "",
            onMore = { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)); },
        ) }

        item { SettingsItem(
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

        item(
            span = { GridItemSpan(this.maxLineSpan) }
        ) {
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

        item(
            span = { GridItemSpan(this.maxLineSpan) }
        ) {
            SettingsPopupHeader(
                title = "Biblio Settings • Home",
                onMore = { onOpenTestScreen()},
            )
        }
        item { SettingsItem(
            title = "Pinned Apps",
            icon = PhosphorIcons.Regular.Pushpin,
            isActivated = null,
            state = when(val pinned = settings.settings.home.pinnedAppsList) {
                emptyList<String>() -> "None"
                else -> pinned.joinToString { it }
            },
            onMore = {},
        ) }
        item { SettingsItem(
            title = "Exact Numbers",
            icon = PhosphorIcons.Regular.Percent,
            isActivated = settings.settings.common.showNumbers,
            state = when(settings.settings.common.showNumbers) {
                true -> "On"
                false -> "Off"
            },
            onToggle = {
                coroutineScope.launch {
                    settings.updateSettings { currentSettings ->
                        currentSettings.toBuilder()
                            .setCommon(currentSettings.common.toBuilder()
                                .setShowNumbers(!currentSettings.common.showNumbers))
                            .build()
                    }
                }
            },
        ) }
    }
}

@Composable
private fun SettingsPopupHeader(
    title: String,
    modifier: Modifier = Modifier,
    onMore: () -> Unit,
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExactText(
            text = title,
            color = BiblioTheme.colors.onBackgroundSecondary,
            modifier = Modifier.weight(1f),
        )
        BiblioButton(
            onClick = onMore,
            rightIcon = PhosphorIcons.Regular.Arrowright,
            text = "More",
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    icon: ImageVector,
    isActivated: Boolean?,
    state: String,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    onToggle: (() -> Unit)? = onMore,
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp, end = 1.dp)
            .clickable(onClick = onToggle ?: onMore ?: {})
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BiblioTheme.colors.onBackgroundSecondary,
            modifier = Modifier
                .then(when(isActivated) {
                    true -> Modifier.background(BiblioTheme.colors.surface, shape = CircleShape)
                    false -> Modifier.border(1.dp, color = BiblioTheme.colors.divider, shape = CircleShape)
                    null -> Modifier
                })
                .padding(6.dp)
                .size(20.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            ExactText(
                text = title,
                maxLines = 1,
            )
            ExactText(
                text = state,
                color = BiblioTheme.colors.onBackgroundSecondary,
                maxLines = 1,
            )
        }
        if(onMore != null) {
            BiblioButton(
                icon = PhosphorIcons.Regular.Caretright,
                onClick = onMore,
            )
        }
    }
}

private data class SettingsSegment(
    val key: String,
    val title: String,
    val icon: ImageVector,
)
@Composable
private fun SettingsSegmentItem(
    title: String,
    segments: List<SettingsSegment>,
    selectedSegmentKey: String,
    modifier: Modifier = Modifier,
    onSelect: (SettingsSegment) -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp),
    ) {
        segments.forEach { segment ->
            val isSelected = segment.key == selectedSegmentKey
            Row(
                modifier = Modifier
                    .border(BiblioTheme.colors.divider, end = 1.dp)
                    .clickable(onClick = { onSelect(segment) })
                    .background(if (isSelected) BiblioTheme.colors.surface else Color.Transparent)
                    .height(48.dp)
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = segment.icon,
                    contentDescription = null,
                    tint = when(isSelected) {
                        true -> BiblioTheme.colors.onBackground
                        false -> BiblioTheme.colors.onBackgroundSecondary
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        BiblioButton(
            onClick = onMore,
            icon = PhosphorIcons.Regular.Caretright,
            modifier = Modifier,
        )
    }
}
package com.andb.apps.biblio.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Pushpin
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.apps.AppItem
import com.andb.apps.biblio.ui.apps.AppsState
import com.andb.apps.biblio.ui.apps.launchApp
import com.andb.apps.biblio.ui.apps.rememberAppsAsState
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun PinnedAppsRow(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val allApps = rememberAppsAsState()
    val pinnedPackages = LocalSettings.current.apps.pinnedApps.value
    val pinnedApps = when (val appsState = allApps.value) {
        is AppsState.Loaded -> appsState.apps
            .filter { it.packageName in pinnedPackages }
        else -> emptyList()
    }
    val isPinnedPopupOpen = remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd,
    ) {
        val maxNumButtons = (this.maxWidth / 32.dp).toInt()
        val showOverflow = pinnedApps.size > maxNumButtons
        val showing = pinnedApps.take(if (showOverflow) maxNumButtons - 1 else maxNumButtons)
        val overflow = if (showOverflow) pinnedApps.drop(maxNumButtons - 1) else emptyList()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            showing.forEach { app ->
                BiblioButton(
                    onClick = { launchApp(app, context) },
                    style = ButtonStyle.Outline,
                    modifier = Modifier.size(32.dp)
                ) {
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            if (showOverflow) {
                BiblioButton(
                    onClick = { isPinnedPopupOpen.value = true },
                    style = ButtonStyle.Outline,
                    icon = PhosphorIcons.Regular.Pushpin,
                )
                if (isPinnedPopupOpen.value) {
                    Popup(
                        alignment = Alignment.BottomCenter,
                        offset = IntOffset(0, with(LocalDensity.current) { -48.dp.roundToPx() }),
                        onDismissRequest = { isPinnedPopupOpen.value = false },
                        properties = PopupProperties()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .border(
                                    1.dp,
                                    color = BiblioTheme.colors.divider,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(
                                    BiblioTheme.colors.background,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            overflow.forEach { app ->
                                AppItem(
                                    app = app,
                                    modifier = Modifier
                                        .clickable { launchApp(app, context) }
                                        .fillMaxWidth(),
                                    isPinned = true,
                                    onPin = {},
                                    onAppInfo = {},
                                    onUninstall = {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
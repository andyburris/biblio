package com.andb.apps.biblio.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Arrowsclockwise
import com.adamglin.phosphoricons.regular.Caretdown
import com.adamglin.phosphoricons.regular.Caretup
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Flask
import com.adamglin.phosphoricons.regular.Gearsix
import com.andb.apps.biblio.BuildConfig
import com.andb.apps.biblio.LibraryView
import com.andb.apps.biblio.SyncApp
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.data.LocalSyncServer
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onOpenTestScreen: () -> Unit,
) {
    val context = LocalContext.current
    val settingsState = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    BiblioScaffold(
        modifier = modifier,
        bottomBar = {
            BiblioBottomBar(
                pageTitle = "Settings",
                onNavigateBack = onNavigateBack,
            ) {
                BiblioButton(
                    style = ButtonStyle.Outline,
                    icon = PhosphorIcons.Regular.Gearsix,
                    text = "Open device settings",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                )
            }
        }
    ) {
        Column {
            if (BuildConfig.DEBUG) {
                SettingsPageItem(
                    title = "Open Test Screen",
                    icon = PhosphorIcons.Regular.Flask,
                    isActivated = null,
                    onMore = onOpenTestScreen
                )
            }

            SettingsPageItem(
                settingState = settingsState.common.showNumbers,
                onToggle = { it.update(!it.value) }
            )

            SettingsPageItem(
                settingState = settingsState.common.eInkColors,
                onToggle = { it.update(!it.value) }
            )

            val isSyncStateExpanded = remember { mutableStateOf(false) }
            SettingsPageItem(
                title = settingsState.common.syncState.name,
                icon = settingsState.common.syncState.icon,
                state = null,
                isActivated = settingsState.common.syncState.isActivated,
                widget = {
                    BiblioButton(
                        onClick = { isSyncStateExpanded.value = !isSyncStateExpanded.value }
                    ) {
                        ExactText(text = settingsState.common.syncState.stateDescription)
                        Icon(
                            imageVector = when(isSyncStateExpanded.value) {
                                true -> PhosphorIcons.Regular.Caretup
                                false -> PhosphorIcons.Regular.Caretdown
                            },
                            contentDescription = when(isSyncStateExpanded.value) {
                                true -> "Expand"
                                false -> "Collapse"
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                content = {
                    when(isSyncStateExpanded.value) {
                        true -> Column(
                            modifier = Modifier.padding(start = 56.dp)
                        ) {
                            SyncApp.entries.filter { it != SyncApp.UNRECOGNIZED }.forEach {
                                BiblioButton(
                                    onClick = {
                                        settingsState.common.syncState.update(settingsState.common.syncState.value.copy(app = it))
                                        isSyncStateExpanded.value = false
                                    }
                                ) {
                                    ExactText(text = settingsState.common.syncState.valueToDescription(
                                        settingsState.common.syncState.value.copy(app = it)
                                    ))
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (settingsState.settings.common.syncApp == it) {
                                        Icon(
                                            imageVector = PhosphorIcons.Regular.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        false -> when(settingsState.settings.common.syncApp) {
                            SyncApp.SYNC_APP_MOON_READER -> Column(
                                modifier = Modifier.padding(start = 64.dp)
                            ) {
                                ExactText("Instructions", color = BiblioTheme.colors.onBackgroundSecondary)
                                InstructionItem(0, "Open Moon Reader", Modifier.padding(end = 16.dp))
                                InstructionItem(1, "Go to “Options”", Modifier.padding(end = 16.dp))
                                InstructionItem(2, "Turn on “Sync via WebDav”", Modifier.padding(end = 16.dp))
                                InstructionItem(3, "In WebDAV sync options, set the URL to:", Modifier.padding(end = 16.dp), widget = {
                                    ExactText(
                                        LocalSyncServer.current.urlFlow.collectAsState().value,
                                        color = BiblioTheme.colors.onBackgroundSecondary
                                    )
                                })
                                InstructionItem(4, "Set the username to:", Modifier.padding(end = 16.dp), widget = {
                                    ExactText("biblio", color = BiblioTheme.colors.onBackgroundSecondary)
                                })
                                InstructionItem(5, "And set the password to:", widget = {
                                    ExactText("a378d4", color = BiblioTheme.colors.onBackgroundSecondary)
                                    BiblioButton(
                                        icon = PhosphorIcons.Regular.Arrowsclockwise,
                                        onClick = {},
                                    )
                                })
                            }
                            SyncApp.SYNC_APP_KOREADER -> Column(
                                modifier = Modifier.padding(start = 64.dp)
                            ) {
                                ExactText("Instructions", color = BiblioTheme.colors.onBackgroundSecondary)

                            }
                            SyncApp.SYNC_APP_NONE, SyncApp.UNRECOGNIZED, null -> {}
                        }
                    }
                }
            )

            SettingsPageItem(
                settingState = settingsState.library.view,
                onToggle = { it.update(when(it.value) {
                    LibraryView.LIBRARY_VIEW_GRID -> LibraryView.LIBRARY_VIEW_SHELVES
                    else -> LibraryView.LIBRARY_VIEW_GRID
                }) }
            )
        }
    }
}

@Composable
fun InstructionItem(
    index: Int,
    instruction: String,
    modifier: Modifier = Modifier,
    widget: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExactText("${index + 1}.",
                Modifier
                    .padding(end = 2.dp)
                    .width(16.dp), textAlign = TextAlign.End)
            ExactText(instruction, Modifier.weight(1f))

        }
        widget?.invoke()
    }
}


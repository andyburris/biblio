package com.andb.apps.biblio.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Pushpin
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Gearsix
import com.adamglin.phosphoricons.regular.Pencilsimple
import com.adamglin.phosphoricons.regular.Pushpin
import com.adamglin.phosphoricons.regular.Trash
import com.andb.apps.biblio.data.LocalSettings
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioButton
import com.andb.apps.biblio.ui.common.pager.BiblioPageSwitcher
import com.andb.apps.biblio.ui.common.pager.BiblioPager
import com.andb.apps.biblio.ui.common.pager.BiblioPagerItem
import com.andb.apps.biblio.ui.common.pager.BiblioPagerWidth
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.ButtonStyle
import com.andb.apps.biblio.ui.common.ExactText
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.theme.BiblioTheme


@Composable
fun AppsPage(
    appsState: AppsState,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings = LocalSettings.current

    val isEditing = remember { mutableStateOf(false) }

    when(appsState) {
        is AppsState.Loaded -> BiblioPager(
            modifier = modifier,
            items = appsState.apps.map { app ->
                BiblioPagerItem(
                    width = BiblioPagerWidth.Dynamic(min = 144.dp),
                    content = {
                        AppItem(
                            app = app,
                            modifier = Modifier
                                .then(when(isEditing.value) {
                                    false -> Modifier.clickable { launchApp(app, context) }
                                    true -> Modifier
                                })
                                .fillMaxSize(),
                            isPinned = app.packageName in settings.apps.pinnedApps.value,
                            isEditing = isEditing.value,
                            onPin = { settings.apps.pinnedApps.update(
                                when(app.packageName in settings.apps.pinnedApps.value) {
                                    true -> settings.apps.pinnedApps.value - app.packageName
                                    false -> settings.apps.pinnedApps.value + app.packageName
                                }
                            ) },
                            onAppInfo = { appsState.openInfo(app) },
                            onUninstall = { appsState.uninstall(app) },
                        )
                    }
                )
            },
            minRowHeight = 64.dp,
            bottomBar = { pagerState ->
                BiblioBottomBar(
                    pageTitle = "Apps",
                    onNavigateBack = onNavigateBack,
                    pageSwitcher = { BiblioPageSwitcher(pagerState) },
                ) {
                    BiblioButton(
                        style = ButtonStyle.Outline,
                        icon = when(isEditing.value) {
                            false -> PhosphorIcons.Regular.Pencilsimple
                            true -> PhosphorIcons.Regular.Check
                        },
                        text = when(isEditing.value) {
                            false -> "Edit"
                            true -> "Done"
                        },
                        onClick = { isEditing.value = !isEditing.value },
                    )
                }
            },
        )
        AppsState.Loading -> BiblioScaffold(
            modifier = modifier,
            bottomBar = { BiblioBottomBar(pageTitle = "Apps", onNavigateBack = onNavigateBack) },
            content = { Text(text = "Loading", modifier = Modifier.padding(16.dp),) }
        )
    }

//    BiblioScaffold(
//        modifier = modifier,
//        bottomBar = {
//            BiblioBottomBar(pageTitle = "Apps", onNavigateBack = onNavigateBack) {
//                BiblioButton(
//                    style = ButtonStyle.Outline,
//                    icon = when(isEditing.value) {
//                        false -> PhosphorIcons.Regular.Pencilsimple
//                        true -> PhosphorIcons.Regular.Check
//                    },
//                    text = when(isEditing.value) {
//                        false -> "Edit"
//                        true -> "Done"
//                    },
//                    onClick = { isEditing.value = !isEditing.value },
//                )
//            }
//        }
//    ) {
//        when(appsState) {
//            AppsState.Loading -> {
//                Text(
//                    text = "Loading",
//                    modifier = Modifier.padding(16.dp),
//                )
//            }
//            is AppsState.Loaded -> {
//                LazyVerticalGrid(
//                    columns = GridCells.Adaptive(144.dp),
//                    modifier = Modifier.fillMaxSize(),
//                ) {
//                    items(appsState.apps) { app ->
//                        AppItem(
//                            app = app,
//                            modifier = Modifier.clickable {
//                                launchApp(app, context)
//                            },
//                            isEditing = isEditing.value,
//                            onPin = { TODO() },
//                            onAppInfo = { appsState.openInfo(app) },
//                            onUninstall = { appsState.uninstall(app)},
//                        )
//                    }
//                }
//            }
//        }
//    }
}

@Composable
fun AppItem(
    app: App,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onPin: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp, end = 1.dp)
            .padding(horizontal = 16.dp)
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(when(isEditing) {
            false -> 12.dp
            true -> 0.dp
        }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = "Icon for ${app.name}",
            modifier = Modifier
                .size(32.dp)
        )
        when(isEditing) {
            false -> ExactText(
                text = app.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            true -> Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy((-8).dp),
            ) {
                ExactText(
                    text = app.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
                Row {
                    BiblioButton(
                        icon = when(isPinned) {
                            false -> PhosphorIcons.Regular.Pushpin
                            true -> PhosphorIcons.Fill.Pushpin
                        },
                        onClick = onPin,
                    )
                    BiblioButton(
                        icon = PhosphorIcons.Regular.Gearsix,
                        onClick = onAppInfo,
                    )

                    if(!app.isSystem) {
                        BiblioButton(
                            icon = PhosphorIcons.Regular.Trash,
                            onClick = onUninstall,
                        )
                    }
                }
            }
        }
    }
}
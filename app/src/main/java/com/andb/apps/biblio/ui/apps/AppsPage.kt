package com.andb.apps.biblio.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.andb.apps.biblio.ui.common.BiblioBottomBar
import com.andb.apps.biblio.ui.common.BiblioScaffold
import com.andb.apps.biblio.ui.common.border
import com.andb.apps.biblio.ui.theme.BiblioTheme

@Composable
fun AppsPage(
    appsState: AppsState,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current

    BiblioScaffold(
        modifier = modifier,
        bottomBar = {
            BiblioBottomBar(pageTitle = "Apps", onNavigateBack = onNavigateBack)
        }
    ) {
        when(appsState) {
            AppsState.Loading -> {
                Text(
                    text = "Loading",
                    modifier = Modifier.padding(16.dp),
                )
            }
            is AppsState.Loaded -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(144.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(appsState.apps) { app ->
                        AppItem(
                            app = app,
                            modifier = Modifier.clickable {
                                launchApp(app, context)
                            })
                    }
                }
            }
        }

    }
}

@Composable
fun AppItem(app: App, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(BiblioTheme.colors.divider, bottom = 1.dp, end = 1.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = "Icon for ${app.name}",
            modifier = Modifier
                .size(32.dp)
        )
        Text(
            text = app.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
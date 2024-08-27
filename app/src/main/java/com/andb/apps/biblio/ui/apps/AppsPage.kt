package com.andb.apps.biblio.ui.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun AppsPage(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val apps = appsAsState()

    BiblioScaffold(
        modifier = modifier,
        bottomBar = {
            BiblioBottomBar(pageTitle = "Apps", onNavigateBack = onNavigateBack)
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(144.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(apps.value) { app ->
                AppItem(
                    app = app,
                    modifier = Modifier.clickable {
                        launchApp(app, context)
                    })
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

data class App(val name: String, val packageName: String, val icon: Drawable)
@Composable
fun appsAsState(): State<List<App>> {
    val context = LocalContext.current
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    val activities: List<ResolveInfo> =
        context.packageManager.queryIntentActivities(intent, 0)
    val installedApps = activities
        .filter { resolveInfo -> resolveInfo.activityInfo.packageName != context.packageName }
        .map { resolveInfo ->
            App(
                name = resolveInfo.loadLabel(context.packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(context.packageManager)
            )
        }
        .sortedBy { app -> app.name }
    return remember {
        mutableStateOf(installedApps)
    }
}

fun launchApp(app: App, context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
    context.startActivity(intent)
}

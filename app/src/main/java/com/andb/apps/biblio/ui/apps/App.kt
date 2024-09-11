package com.andb.apps.biblio.ui.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

sealed class AppsState {
    data object Loading : AppsState()
    data class Loaded(val apps: List<App>) : AppsState()
}

data class App(val name: String, val packageName: String, val icon: Drawable)

@Composable
fun rememberAppsAsState(): State<AppsState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<AppsState>(AppsState.Loading) }

    LaunchedEffect(Unit) {
        state.value = AppsState.Loaded(loadApps(context))
    }

    val installBroadcastReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) return
                state.value = AppsState.Loaded(loadApps(context))
            }
        }
    }

    DisposableEffect(Unit) {
        context.registerReceiver(
            installBroadcastReceiver,
            IntentFilter(Intent.ACTION_PACKAGE_ADDED).also {
                it.addAction(Intent.ACTION_PACKAGE_CHANGED)
                it.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            },
        )
        onDispose {
            context.unregisterReceiver(installBroadcastReceiver)
        }
    }

    return state
}

fun launchApp(app: App, context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
    context.startActivity(intent)
}

private fun loadApps(context: Context): List<App> {
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
    return installedApps
}
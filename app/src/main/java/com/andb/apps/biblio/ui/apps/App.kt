package com.andb.apps.biblio.ui.apps

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

sealed class AppsState {
    data object Loading : AppsState()
    data class Loaded(
        val apps: List<App>,
        private val context: Context,
    ) : AppsState() {
        fun openInfo(app: App) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:${app.packageName}"))
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace();
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                context.startActivity(intent)
            }
        }

        fun uninstall(app: App) {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:${app.packageName}")
            context.startActivity(intent)
        }
    }
}

data class App(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystem: Boolean,
)

@Composable
fun rememberAppsAsState(): State<AppsState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<AppsState>(AppsState.Loading) }

    LaunchedEffect(Unit) {
        state.value = AppsState.Loaded(loadApps(context), context)
    }

    val installBroadcastReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) return
                state.value = AppsState.Loaded(loadApps(context), context)
            }
        }
    }

    DisposableEffect(Unit) {
        context.registerReceiver(
            installBroadcastReceiver,
            IntentFilter(Intent.ACTION_PACKAGE_ADDED).also {
                it.addAction(Intent.ACTION_PACKAGE_CHANGED)
                it.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
                it.addDataScheme("package")
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
                icon = resolveInfo.loadIcon(context.packageManager),
                isSystem = resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            )
        }
        .sortedBy { app -> app.name }
    return installedApps
}
package com.andb.apps.biblio.ui.home

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.andb.apps.biblio.BuildConfig
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

data class StoragePermissionState(
    val isGranted: Boolean,
    val launchPermissionRequest: () -> Unit,
)

private val RETURN_URI = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
private val needsExtraPermission = VERSION.SDK_INT > VERSION_CODES.Q

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberStoragePermissionState(): StoragePermissionState {
    val permissionState = rememberPermissionState(permission = android.Manifest.permission.READ_EXTERNAL_STORAGE)

    fun isGranted(): Boolean {
        return when(needsExtraPermission) {
            true -> isExternalStorageManager()
            false -> permissionState.status.isGranted
        }
    }
    val isGrantedState = remember(permissionState.status.isGranted) { mutableStateOf(isGranted()) }
    val activityResult = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        isGrantedState.value = isGranted()
    }


    return StoragePermissionState(
        isGranted = isGrantedState.value,
        launchPermissionRequest = {
            when(needsExtraPermission) {
                true -> activityResult.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, RETURN_URI))
                false -> permissionState.launchPermissionRequest()
            }
        }
    )
}

@TargetApi(VERSION_CODES.R)
fun isExternalStorageManager(): Boolean {
    return Environment.isExternalStorageManager()
}
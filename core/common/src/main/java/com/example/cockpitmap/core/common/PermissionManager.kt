package com.example.cockpitmap.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat

/**
 * 车载系统权限管理器。
 * 
 * [职责描述]：
 * 1. 集中管理地图业务所需的所有权限列表。
 * 2. 提供 Compose 友好的权限请求挂钩。
 */
object PermissionManager {

    /**
     * 地图业务运行所需的核心权限清单。
     */
    val MAP_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.FOREGROUND_SERVICE_LOCATION
    )

    /**
     * 检查当前是否已获得全部地图所需权限。
     */
    fun hasAllPermissions(context: Context): Boolean {
        return MAP_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * 权限请求发起器（Composable 版）。
 * 
 * @param onAllGranted 当所有权限均被授予时的回调。
 */
@Composable
fun CockpitPermissionRequester(onAllGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            onAllGranted()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(PermissionManager.MAP_PERMISSIONS)
    }
}

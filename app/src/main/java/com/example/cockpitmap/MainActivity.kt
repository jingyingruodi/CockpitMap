package com.example.cockpitmap

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.feature.map.MapController
import com.example.cockpitmap.feature.map.MapRenderScreen

/**
 * 应用程序主 Activity。
 * 
 * [修改说明]：
 * 优化了权限申请流程。现在应用会先确保权限授予，再加载地图组件。
 * 这解决了高德 SDK 在无权限状态下初始化定位引擎导致后续无法定位的问题。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCockpitTheme {
                // 确保权限在 [MainScreen] 实例化之前请求
                var permissionsGranted by remember { mutableStateOf(false) }
                
                PermissionRequester(onAllGranted = {
                    permissionsGranted = true
                })

                if (permissionsGranted) {
                    MainScreen()
                } else {
                    // 权限请求期间显示背景占位，防止提前初始化 SDK
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {}
                }
            }
        }
    }
}

/**
 * 运行时权限请求组件
 */
@Composable
fun PermissionRequester(onAllGranted: () -> Unit) {
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            onAllGranted()
        } else {
            Log.e("MainActivity", "用户拒绝了核心权限，定位功能将受限")
            // 依然通知外部可以启动，但 SDK 内部会降级
            onAllGranted() 
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }
}

@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/**
 * 主屏幕组合容器
 */
@Composable
fun MainScreen() {
    var mapController by remember { mutableStateOf<MapController?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 此时权限已通过，Safe 加载地图
            MapRenderScreen(
                modifier = Modifier.fillMaxSize(),
                initialLocation = GeoLocation(39.9042, 116.4074, "默认点"),
                onControllerReady = { controller ->
                    mapController = controller
                }
            )

            // UI 面板
            NavigationPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 48.dp)
                    .width(360.dp)
            )

            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                onZoomIn = { mapController?.zoomIn() },
                onZoomOut = { mapController?.zoomOut() },
                onLocateMe = { mapController?.locateMe() }
            )

            VoiceStatusBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun NavigationPanel(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Menu, contentDescription = "设置")
                Spacer(Modifier.width(16.dp))
                Text("输入目的地...", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("常用推荐：", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text("🏠 回家", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Text("🏢 公司", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun QuickActions(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onLocateMe: () -> Unit = {}
) {
    Column(modifier = modifier) {
        FloatingActionButton(onClick = onZoomIn, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = onZoomOut, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Text("-", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = onLocateMe) {
            Icon(Icons.Default.MyLocation, contentDescription = "居中定位")
        }
    }
}

@Composable
fun VoiceStatusBar(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(0.5f),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("✨ 系统已准备就绪", color = Color.White)
        }
    }
}

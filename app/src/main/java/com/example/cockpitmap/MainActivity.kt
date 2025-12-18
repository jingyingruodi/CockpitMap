package com.example.cockpitmap

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cockpitmap.core.data.repository.LocationRepository
import com.example.cockpitmap.feature.map.MapController
import com.example.cockpitmap.feature.map.MapRenderScreen
import kotlinx.coroutines.launch

/**
 * 应用程序主入口 Activity。
 * 
 * [修改说明]：
 * 1. 修复了由于缺失 :core:data 依赖导致的编译失败。
 * 2. 修正了 Compose 布局参数中 Alignment 与 Modifier 的类型冲突。
 * 3. 补全了缺失的 Import (dp, MyLocation 图标等)。
 */
class MainActivity : ComponentActivity() {
    
    /**
     * 持久化位置数据仓库。
     * 单例模式，确保整个应用生命周期内 DataStore 访问的一致性。
     */
    private val locationRepository by lazy { LocationRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCockpitTheme {
                // 权限授予状态
                var permissionsGranted by remember { mutableStateOf(false) }
                
                // 从 Repository 读取缓存位置
                val lastKnownLoc by locationRepository.lastKnownLocation.collectAsState(initial = null)

                // 先行请求权限
                PermissionRequester(onAllGranted = {
                    permissionsGranted = true
                })

                if (permissionsGranted) {
                    MainScreen(
                        repository = locationRepository,
                        cachedLocation = lastKnownLoc
                    )
                } else {
                    // 等待权限时显示深色占位，避免白屏
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {}
                }
            }
        }
    }
}

/**
 * 运行时权限请求组件。
 */
@Composable
fun PermissionRequester(onAllGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> onAllGranted() }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        ))
    }
}

/**
 * 主题配置。
 */
@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content
    )
}

/**
 * 主屏幕 UI 组合容器。
 */
@Composable
fun MainScreen(
    repository: LocationRepository,
    cachedLocation: com.example.cockpitmap.core.model.GeoLocation?
) {
    var mapController by remember { mutableStateOf<MapController?>(null) }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 地图渲染核心
            MapRenderScreen(
                modifier = Modifier.fillMaxSize(),
                initialLocation = cachedLocation,
                onLocationChanged = { newLoc ->
                    // 每当捕获新坐标，异步存入 DataStore
                    scope.launch { repository.saveLastLocation(newLoc) }
                },
                onControllerReady = { controller -> mapController = controller }
            )

            // 右侧快捷操作按钮
            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                onZoomIn = { mapController?.zoomIn() },
                onZoomOut = { mapController?.zoomOut() },
                onLocateMe = { mapController?.locateMe() }
            )
        }
    }
}

/**
 * 地图快捷控制按钮组。
 */
@Composable
fun QuickActions(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocateMe: () -> Unit
) {
    Column(modifier = modifier) {
        FloatingActionButton(
            onClick = onZoomIn,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(
            onClick = onZoomOut,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text("-", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = onLocateMe) {
            Icon(Icons.Default.MyLocation, contentDescription = "定位")
        }
    }
}

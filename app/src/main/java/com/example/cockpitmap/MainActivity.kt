package com.example.cockpitmap

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cockpitmap.core.data.repository.LocationRepository
import com.example.cockpitmap.core.model.CustomMapStyle
import com.example.cockpitmap.core.model.MapController
import com.example.cockpitmap.feature.map.MapRenderScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 应用程序主入口 Activity。
 * 
 * [修正重点]：
 * 1. 同步了 CustomMapStyle 和 MapController 的下沉路径。
 * 2. 彻底清理了代码中的非法字符标记。
 * 3. 规范了 Compose 状态观察语法。
 */
class MainActivity : ComponentActivity() {
    
    private val locationRepository by lazy { LocationRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCockpitTheme {
                var permissionsGranted by remember { mutableStateOf(false) }
                // 观察缓存位置
                val lastKnownLoc by locationRepository.lastKnownLocation.collectAsState(initial = null)

                PermissionRequester(onAllGranted = {
                    permissionsGranted = true
                })

                if (permissionsGranted) {
                    MainScreen(
                        repository = locationRepository,
                        cachedLocation = lastKnownLoc
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {}
                }
            }
        }
    }
}

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

@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content
    )
}

@Composable
fun MainScreen(
    repository: LocationRepository,
    cachedLocation: com.example.cockpitmap.core.model.GeoLocation?
) {
    // 修正：去除非法 handle 占位符
    var mapController by remember { mutableStateOf<MapController?>(null) }
    val scope = rememberCoroutineScope()
    
    // 样式循环切换逻辑：使用 entries 获取枚举列表
    val styles = CustomMapStyle.entries.toTypedArray()
    val styleNames = listOf("标准模式", "夜间模式", "卫星模式", "导航模式")
    var currentStyleIndex by remember { mutableIntStateOf(1) } // 默认 NIGHT
    
    var showStyleHint by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapRenderScreen(
                modifier = Modifier.fillMaxSize(),
                initialLocation = cachedLocation,
                onLocationChanged = { newLoc ->
                    scope.launch { repository.saveLastLocation(newLoc) }
                },
                onControllerReady = { controller -> mapController = controller }
            )

            // 样式切换提示卡片
            AnimatedVisibility(
                visible = showStyleHint,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 160.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = hintText,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                onZoomIn = { mapController?.zoomIn() },
                onZoomOut = { mapController?.zoomOut() },
                onLocateMe = { mapController?.locateMe() },
                onSwitchStyle = {
                    currentStyleIndex = (currentStyleIndex + 1) % styles.size
                    val newStyle = styles[currentStyleIndex]
                    // 修正：调用接口中定义的 Int 类型 type
                    mapController?.setMapStyle(newStyle.type)
                    
                    hintText = "已切换至：${styleNames[currentStyleIndex]}"
                    scope.launch {
                        showStyleHint = true
                        delay(2000)
                        showStyleHint = false
                    }
                }
            )
        }
    }
}

@Composable
fun QuickActions(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocateMe: () -> Unit,
    onSwitchStyle: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(onClick = onZoomIn) { Text("+") }
        Spacer(Modifier.height(12.dp))
        FloatingActionButton(onClick = onZoomOut) { Text("-") }
        Spacer(Modifier.height(12.dp))
        
        FloatingActionButton(
            onClick = onSwitchStyle,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Icon(Icons.Default.Layers, contentDescription = "样式")
        }
        
        Spacer(Modifier.height(12.dp))
        FloatingActionButton(onClick = onLocateMe) {
            Icon(Icons.Default.MyLocation, contentDescription = "定位")
        }
    }
}

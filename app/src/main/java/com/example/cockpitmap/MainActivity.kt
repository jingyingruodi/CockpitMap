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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
// 引用核心模型与地图功能模块
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.feature.map.MapRenderScreen

/**
 * [CockpitMap] 项目主入口
 * 
 * 架构设计守则：
 * 1. 本 Activity 作为“壳”，仅负责各 feature 模块的拼装与系统级权限调度。
 * 2. UI 采用沉浸式全屏布局，适配车载宽屏/横屏。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 开启全屏边缘到边缘显示，确保地图充满整个屏幕
        enableEdgeToEdge()
        
        setContent {
            SimpleCockpitTheme {
                // 启动权限检查流程
                PermissionRequester {
                    MainScreen()
                }
            }
        }
    }
}

/**
 * 运行时权限请求组件
 * 
 * 修复 bug: 解决卸载重装后不申请权限导致高德 SDK 定位失败的问题。
 */
@Composable
fun PermissionRequester(onGranted: @Composable () -> Unit) {
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // 权限申请结果处理逻辑（此处简化，实际生产环境可增加引导说明）
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    onGranted()
}

/**
 * 车机基础主题配置
 * 
 * 视觉守则：
 * 1. 优先使用 Dark 模式，减少驾驶员夜间视觉疲劳。
 * 2. 使用 Material 3 规范以获得更好的动态配色支持。
 */
@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * 主屏幕组合容器
 * 
 * HMI 布局逻辑：
 * - 底部：地图渲染层 (MapRenderScreen)
 * - 左侧：驾驶员操作面板 (NavigationPanel)
 * - 右侧：快捷工具栏 (QuickActions)
 * - 中下：语音交互栏 (VoiceStatusBar)
 */
@Composable
fun MainScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // --- 核心地图渲染层 ---
            // 调用自 [:feature:map] 模块，传入北京作为默认预览位置
            MapRenderScreen(
                modifier = Modifier.fillMaxSize(),
                initialLocation = GeoLocation(39.9042, 116.4074, "天安门")
            )

            // --- 导航搜索面板 (悬浮) ---
            // 位置：左上角，距离左边 24dp，方便左舵车主盲操
            NavigationPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 48.dp)
                    .width(360.dp)
            )

            // --- 快捷操作控制 (悬浮) ---
            // 位置：右侧中心，采用大尺寸 FAB (FloatingActionButton) 确保安全点击
            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )

            // --- 语音助手状态栏 (悬浮) ---
            // 位置：底部中央，采用胶囊型卡片减少对地图路径的遮挡
            VoiceStatusBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

/**
 * 搜索与常用目的地面板
 */
@Composable
fun NavigationPanel(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Menu, contentDescription = "菜单")
                Spacer(Modifier.width(16.dp))
                Text("输入目的地...", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("快速前往：", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text("🏠 回家 (15分钟)", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Text("🏢 去公司 (35分钟)", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * 地图工具栏（缩放/定位）
 * 
 * 安全规范：车载环境下的按钮尺寸必须大于 48dp (此处使用 56dp+ 容器)
 */
@Composable
fun QuickActions(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        FloatingActionButton(
            onClick = {}, 
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(
            onClick = {}, 
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text("-", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = {}) {
            Icon(Icons.Default.Mic, contentDescription = "语音/定位")
        }
    }
}

/**
 * 语音助手状态栏
 */
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
            Icon(Icons.Default.Mic, contentDescription = "语音", tint = Color.Cyan)
            Spacer(Modifier.width(16.dp))
            Text("正在倾听...", color = Color.White)
        }
    }
}

package com.example.cockpitmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
// 引入核心模型和地图功能组件
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.feature.map.MapRenderScreen

/**
 * 应用程序主入口 Activity。
 * 采用全屏沉浸式设计，适配车机横屏/宽屏显示。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 开启全屏边缘到边缘显示，确保地图充满整个屏幕
        enableEdgeToEdge()
        
        setContent {
            SimpleCockpitTheme {
                MainScreen()
            }
        }
    }
}

/**
 * 车机基础主题配置
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
 * 主屏幕布局。
 * 按照 [MODULES.md] 规范，将 feature 模块的组件组合在一起。
 */
@Composable
fun MainScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // --- 区域 1: 核心地图渲染层 ---
            // 调用 [feature:map] 模块提供的组件
            MapRenderScreen(
                modifier = Modifier.fillMaxSize(),
                initialLocation = GeoLocation(39.9042, 116.4074, "北京") // 模拟初始位置
            )

            // --- 区域 2: 导航搜索面板 (悬浮) ---
            NavigationPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 48.dp)
                    .width(360.dp)
            )

            // --- 区域 3: 快捷操作控制 (悬浮) ---
            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )

            // --- 区域 4: 语音助手状态栏 (悬浮) ---
            VoiceStatusBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

/**
 * 搜索与目的地快捷面板 (UI 组件)
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
                Icon(Icons.Default.Menu, contentDescription = "设置")
                Spacer(Modifier.width(16.dp))
                Text("寻找目的地...", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("常用：", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text("🏠 回家 (预计15分钟)", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Text("🏢 公司 (预计35分钟)", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * 地图操作快捷按钮组 (UI 组件)
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
            Icon(Icons.Default.Mic, contentDescription = "语音助手")
        }
    }
}

/**
 * 语音交互状态展示栏 (UI 组件)
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
            Icon(Icons.Default.Mic, contentDescription = "语音波形", tint = Color.Cyan)
            Spacer(Modifier.width(16.dp))
            Text("你好，请问想去哪里？", color = Color.White)
        }
    }
}

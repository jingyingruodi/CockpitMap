package com.example.cockpitmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
 * 未来可扩展为从 [core:designsystem] 获取统一的配色方案
 */
@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    // 车机环境建议优先使用深色模式以减少夜间驾驶炫光
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * 主屏幕布局。
 * 采用层叠布局 (Box)，底层为地图，上层悬浮交互组件。
 */
@Composable
fun MainScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // --- 区域 1: 地图底座 ---
            // 占位符，未来接入 feature:map 模块的地图渲染组件
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C3E50)),
                contentAlignment = Alignment.Center
            ) {
                Text("地图图层加载中...", color = Color.LightGray)
            }

            // --- 区域 2: 导航搜索面板 ---
            // 放置在屏幕左侧，靠近驾驶员一侧，方便盲操
            NavigationPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 48.dp)
                    .width(360.dp)
            )

            // --- 区域 3: 快捷操作控制 ---
            // 右侧垂直排列大按钮，用于缩放地图和触发关键操作
            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )

            // --- 區域 4: 语音助手状态栏 ---
            // 底部居中展示，减少对地图路径遮挡
            VoiceStatusBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

/**
 * 搜索与目的地快捷面板
 */
@Composable
fun NavigationPanel(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        // 半透明背景，确保能隐约看到地图背景
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
 * 地图操作快捷按钮组
 */
@Composable
fun QuickActions(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // 放大按钮：车机按钮需保持足够大的点击区域 (>= 64dp 推荐)
        FloatingActionButton(
            onClick = {}, 
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        // 缩小按钮
        FloatingActionButton(
            onClick = {}, 
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text("-", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        // 语音唤醒/定位按钮
        FloatingActionButton(onClick = {}) {
            Icon(Icons.Default.Mic, contentDescription = "语音助手")
        }
    }
}

/**
 * 语音交互状态展示栏
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

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCockpitTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun MainScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 地图背景占位
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C3E50)),
                contentAlignment = Alignment.Center
            ) {
                Text("地图图层占位", color = Color.LightGray)
            }

            // 2. 左侧悬浮搜索面板
            NavigationPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 48.dp)
                    .width(360.dp)
            )

            // 3. 右侧快捷操作区
            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )

            // 4. 底部语音交互/状态栏
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
                Icon(Icons.Default.Menu, contentDescription = "菜单")
                Spacer(Modifier.width(16.dp))
                Text("输入目的地...", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("最近目的地：", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text("🏠 回家 (15分钟)", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text("🏢 去公司 (35分钟)", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun QuickActions(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        FloatingActionButton(onClick = {}, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = {}, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Text("-", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = {}) {
            Icon(Icons.Default.Mic, contentDescription = "语音")
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
            Icon(Icons.Default.Mic, contentDescription = "语音", tint = Color.Cyan)
            Spacer(Modifier.width(16.dp))
            Text("正在倾听...", color = Color.White)
        }
    }
}

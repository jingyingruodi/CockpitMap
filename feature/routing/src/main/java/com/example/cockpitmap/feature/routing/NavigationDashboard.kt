package com.example.cockpitmap.feature.routing

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cockpitmap.core.designsystem.CockpitSurface

/**
 * 导航实时引导面板。
 * 
 * [职责描述]：
 * 处于正式导航模式时，在屏幕顶部显示状态并提供退出入口。
 */
@Composable
fun NavigationDashboard(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    CockpitSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = Color.Black.copy(alpha = 0.85f) // 导航时加深背景以提升对比度
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 模拟简单的引导信息
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("直", color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = "正在导航中", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(text = "请沿当前道路继续行驶", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
            }

            // 退出导航按钮
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("退出", fontSize = 14.sp)
            }
        }
    }
}

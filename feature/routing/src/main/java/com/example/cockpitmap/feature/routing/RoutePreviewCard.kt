package com.example.cockpitmap.feature.routing

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.RouteInfo

/**
 * 导航路径预览卡片。
 * 
 * 展示路径规划后的概要信息（距离、时间），并提供“开始导航”入口。
 */
@Composable
fun RoutePreviewCard(
    routeInfo: RouteInfo,
    onStartNavigation: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CockpitSurface(
        modifier = modifier
            .width(400.dp)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "预计耗时: ${routeInfo.duration / 60} 分钟",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "全程约 ${(routeInfo.distance / 1000).format(1)} 公里 | ${routeInfo.strategy}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = onStartNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "开始导航", fontSize = 18.sp)
                }
            }

            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(text = "取消", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

/**
 * 格式化 Float 扩展函数。
 */
private fun Float.format(digits: Int) = "%.${digits}f".format(this)

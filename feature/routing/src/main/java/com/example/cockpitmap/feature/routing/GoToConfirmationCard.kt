package com.example.cockpitmap.feature.routing

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.GeoLocation

/**
 * 前往目的地确认卡片（极致瘦身版）。
 * 
 * 展示路径规划前的确认信息，并支持收藏状态感应。
 */
@Composable
fun GoToConfirmationCard(
    location: GeoLocation,
    isFavorited: Boolean, // [新增]：是否已收藏
    onConfirm: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CockpitSurface(
        modifier = modifier
            .width(280.dp)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = "目的地",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = location.name ?: "未知地点",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 取消
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(text = "取消", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }
                
                Spacer(Modifier.weight(1f))

                // 收藏按钮：根据状态切换实心/空心图标
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                        contentDescription = null, 
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 前往
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = "去这里", fontSize = 13.sp)
                }
            }
        }
    }
}

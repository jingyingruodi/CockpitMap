package com.example.cockpitmap.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 车机专用标准悬浮卡片。
 * 
 * [HMI 规范]：
 * - 背景：采用半透明深色 (alpha 0.7) 以保证在地图背景上的可读性。
 * - 圆角：24dp，符合圆润的现代化 HMI 视觉趋势。
 */
@Composable
fun CockpitSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.7f),
        contentColor = Color.White
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            content()
        }
    }
}

/**
 * 车机专用操作按钮。
 * 
 * [功能说明]：
 * 统一了圆角和海拔高度，移除阴影以适应扁平化的车载界面。
 * 
 * @param onClick 点击事件回调。
 * @param icon 按钮图标 Composable。
 * @param containerColor 按钮背景色，默认为主题主色容器。
 */
@Composable
fun CockpitFloatingButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
    ) {
        icon()
    }
}

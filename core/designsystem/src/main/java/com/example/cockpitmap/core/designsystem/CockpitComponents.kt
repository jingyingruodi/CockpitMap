package com.example.cockpitmap.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 车机专用标准悬浮卡片。
 * 
 * [HMI 规范]：
 * - 背景：默认采用半透明深色 (alpha 0.7) 以保证在地图背景上的可读性。
 * - 圆角：默认 24dp，符合圆润的现代化 HMI 视觉趋势。
 */
@Composable
fun CockpitSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    color: Color = Color.Black.copy(alpha = 0.7f),
    contentColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            content()
        }
    }
}

/**
 * 车机专用标准悬浮按钮。
 */
@Composable
fun CockpitFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black.copy(alpha = 0.7f),
    contentColor: Color = Color.White,
    icon: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        icon()
    }
}

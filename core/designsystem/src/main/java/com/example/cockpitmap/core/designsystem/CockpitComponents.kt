package com.example.cockpitmap.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 车机专用标准悬浮卡片。
 * 
 * [职责描述]：
 * 提供符合车载 HMI 规范的半透明深色容器，具备大圆角和标准内边距。
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
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

/**
 * 车机专用标准悬浮按钮。
 * 
 * [职责描述]：
 * 提供大点击区域的悬浮按钮，支持自定义容器颜色。
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
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        icon()
    }
}

/**
 * 车机顶部全局提示层。
 * 
 * [职责描述]：
 * 统一展示系统级提示信息（如样式切换、保存成功等）。
 */
@Composable
fun CockpitHintLayer(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(visible = visible) {
            CockpitSurface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(text = text, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * 车机全局加载遮罩。
 * 
 * [职责描述]：
 * 提供统一的业务加载（如路径规划中）的视觉反馈。
 */
@Composable
fun CockpitLoadingHint(
    visible: Boolean,
    text: String = "正在处理...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible) {
        CockpitSurface(modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(text = text, fontSize = 13.sp)
            }
        }
    }
}

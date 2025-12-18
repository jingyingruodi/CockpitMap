package com.example.cockpitmap.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.cockpitmap.core.model.GeoLocation

/**
 * 地图功能模块的主要显示界面。
 * 
 * 按照 [MODULES.md] 的规范：
 * 1. 业务逻辑与渲染高度内聚在此模块。
 * 2. 对外暴露 Composable 组件供 [app] 模块调用。
 * 3. 核心数据模型引用自 [:core:model]。
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null
) {
    // 这里是未来集成 高德/百度/Mapbox SDK 的地方。
    // 目前使用占位 UI 模拟地图渲染层。
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B2631)), // 使用更深的地磁蓝色作为模拟地图基调
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Map Engine Render Area",
                color = Color.Cyan.copy(alpha = 0.6f)
            )
            initialLocation?.let {
                Text(
                    text = "定位中: ${it.latitude}, ${it.longitude}",
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * 模拟地图控制接口。
 * 协作规则：通过 ViewModel 或 State 驱动地图动作。
 */
interface MapController {
    fun zoomIn()
    fun zoomOut()
    fun moveTo(location: GeoLocation)
}

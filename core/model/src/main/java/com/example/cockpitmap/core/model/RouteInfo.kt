package com.example.cockpitmap.core.model

/**
 * 路径规划结果模型。
 */
data class RouteInfo(
    val id: String,
    val distance: Float,      // 距离（米）
    val duration: Long,       // 耗时（秒）
    val polyline: List<GeoLocation>, // 路径坐标点集合
    val strategy: String = "速度优先" // 规划策略
)

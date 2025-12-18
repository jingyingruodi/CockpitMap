package com.example.cockpitmap.core.model

/**
 * 核心地理位置模型。
 * 该模型定义在 [core:model] 模块，不依赖任何第三方地图 SDK，确保业务逻辑的通用性。
 * 
 * @property latitude 纬度
 * @property longitude 经度
 * @property name 地点名称（可选）
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
)

/**
 * 地图交互状态枚举
 */
enum class MapViewMode {
    FOLLOWING, // 随车转动/跟随模式
    NORTH_UP,  // 北向上模式
    OVERVIEW   // 全览模式
}

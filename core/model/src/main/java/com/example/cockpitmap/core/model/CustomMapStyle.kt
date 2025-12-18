package com.example.cockpitmap.core.model

/**
 * 地图样式枚举（下沉至 core:model 保证全项目可见）。
 * 对应高德 SDK 的内置样式常量。
 */
enum class CustomMapStyle(val type: Int) {
    NORMAL(1),    // AMap.MAP_TYPE_NORMAL
    NIGHT(2),     // AMap.MAP_TYPE_NIGHT
    SATELLITE(3), // AMap.MAP_TYPE_SATELLITE
    NAVI(4)       // AMap.MAP_TYPE_NAVI
}

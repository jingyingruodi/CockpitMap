package com.example.cockpitmap.core.model

/**
 * 地图样式枚举。
 * 
 * [修正说明]：
 * 严格对齐 AMap SDK 官方常量值：
 * 1 -> NORMAL (标准)
 * 2 -> SATELLITE (卫星)
 * 3 -> NIGHT (夜间)
 * 4 -> NAVI (导航)
 */
enum class CustomMapStyle(val type: Int) {
    NORMAL(1),
    SATELLITE(2),
    NIGHT(3),
    NAVI(4)
}

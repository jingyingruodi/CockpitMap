package com.example.cockpitmap.core.model

/**
 * 表示用户保存的常用地点。
 */
data class SavedLocation(
    val id: String,
    val name: String,
    val type: LocationType,
    val location: GeoLocation,
    val address: String = ""
)

/**
 * 常用地点的分类。
 */
enum class LocationType {
    HOME,       // 家
    OFFICE,     // 公司
    FAVORITE,   // 收藏
    CUSTOM      // 自定义
}

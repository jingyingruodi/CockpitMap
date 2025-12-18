package com.example.cockpitmap.core.model

/**
 * 通用地理位置领域模型。
 * 
 * 职责：
 * 屏蔽 SDK 特有的坐标类（如 AMap LatLng），作为本项目各层级之间流转的唯一坐标载体。
 * 
 * @property latitude 纬度 (Double)
 * @property longitude 经度 (Double)
 * @property name 地点显示名称（可选，如“当前位置”）
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
)

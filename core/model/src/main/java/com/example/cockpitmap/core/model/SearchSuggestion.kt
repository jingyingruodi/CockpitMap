package com.example.cockpitmap.core.model

/**
 * 搜索建议模型
 * 
 * 用于在搜索输入框下方展示高德 POI 联想结果。
 * 
 * @property id POI 唯一标识
 * @property title 地点名称（如：王府井）
 * @property snippet 地点详细地址（如：北京市东城区）
 * @property location 经纬度信息
 */
data class SearchSuggestion(
    val id: String,
    val title: String,
    val snippet: String,
    val location: GeoLocation? = null
)

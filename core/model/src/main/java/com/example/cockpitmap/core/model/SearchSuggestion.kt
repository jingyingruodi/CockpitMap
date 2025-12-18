package com.example.cockpitmap.core.model

/**
 * 搜索联想建议数据模型。
 * 
 * 职责：
 * 承载高德搜索接口返回的地点联想信息，用于在 POI 搜索列表中展示。
 * 
 * @property id POI 唯一标识码（对应 AMap adcode/id）
 * @property title 地点主标题（如：北京大学）
 * @property snippet 地点副标题/地址描述（如：北京市海淀区颐和园路5号）
 * @property location 该建议点对应的坐标（可选，部分搜索结果可能无坐标）
 */
data class SearchSuggestion(
    val id: String,
    val title: String,
    val snippet: String,
    val location: GeoLocation? = null
)

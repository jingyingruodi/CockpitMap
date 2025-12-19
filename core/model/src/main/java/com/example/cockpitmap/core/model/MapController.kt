package com.example.cockpitmap.core.model

/**
 * 地图操作控制接口。
 * 
 * [下沉说明]：
 * 按照 [MODULES.md] 规范，该接口定义在 core:model 模块。
 * 目的：允许 :feature:routing 等模块在不依赖 :feature:map 模块的情况下，
 * 能够调用地图控制功能（如 moveTo），实现模块间彻底解耦。
 */
interface MapController {
    /** 放大 */
    fun zoomIn()
    /** 缩小 */
    fun zoomOut()
    /** 镜头瞬移/平移至指定坐标 */
    fun moveTo(location: GeoLocation)
    /** 在地图上显示一个标记点（如搜索结果） */
    fun showMarker(location: GeoLocation)
    /** 清除所有标记点 */
    fun clearMarkers()
    /** 触发“回到我的位置” */
    fun locateMe()
    /** 设置地图显示样式（NORMAL, NIGHT, SATELLITE, NAVI） */
    fun setMapStyle(type: Int)
    /** 设置标记点长按监听 */
    fun setOnMarkerLongClickListener(onLongClick: (GeoLocation) -> Unit)
}

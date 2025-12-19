package com.example.cockpitmap.core.model

/**
 * 地图操作控制接口。
 * 
 * [职责描述]：
 * 定义跨模块的地图控制能力，包括视角切换、标注管理及路径绘制。
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
    /** 绘制导航路径 */
    fun drawRoute(route: RouteInfo)
    /** 清除导航路径 */
    fun clearRoute()
    /** 触发“回到我的位置” */
    fun locateMe()
    /** 设置地图显示样式（NORMAL, NIGHT, SATELLITE, NAVI） */
    fun setMapStyle(type: Int)
    /** 设置标记点长按监听 */
    fun setOnMarkerLongClickListener(onLongClick: (GeoLocation) -> Unit)
    
    /** 
     * 切换导航跟随模式。
     * @param enabled true: 开启车头向上、3D 倾斜视角；false: 回到正北朝上平视。
     */
    fun setFollowingMode(enabled: Boolean)
}

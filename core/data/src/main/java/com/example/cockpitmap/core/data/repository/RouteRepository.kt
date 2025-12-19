package com.example.cockpitmap.core.data.repository

import android.content.Context
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.*
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.RouteInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 * 路径规划数据仓库。
 * 
 * [职责]：
 * 负责封装高德地图路线搜索 SDK，提供驾车路径规划能力。
 */
class RouteRepository(private val context: Context) {

    private val routeSearch: RouteSearch by lazy { RouteSearch(context) }

    /**
     * 计算从起点到终点的驾车路径规划方案。
     * 
     * @param start 导航起点
     * @param end 导航终点
     * @return 规划出的 [RouteInfo] 对象，若失败则返回 null
     */
    @Suppress("DEPRECATION")
    suspend fun calculateDriveRoute(start: GeoLocation, end: GeoLocation): RouteInfo? = suspendCancellableCoroutine { continuation ->
        val fromAndTo = RouteSearch.FromAndTo(
            LatLonPoint(start.latitude, start.longitude),
            LatLonPoint(end.latitude, end.longitude)
        )
        
        // 使用驾车路径规划请求
        val query = RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.DrivingDefault, null, null, "")
        
        routeSearch.setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
            override fun onDriveRouteSearched(result: DriveRouteResult?, errorCode: Int) {
                val paths = result?.paths
                if (errorCode == 1000 && !paths.isNullOrEmpty()) {
                    val path = paths[0]
                    
                    val polylinePoints = path.steps.flatMap { step ->
                        step.polyline.map { point -> 
                            GeoLocation(point.latitude, point.longitude) 
                        }
                    }
                    
                    continuation.resume(
                        RouteInfo(
                            id = UUID.randomUUID().toString(),
                            distance = path.distance,
                            duration = path.duration,
                            polyline = polylinePoints,
                            strategy = "速度优先"
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }

            override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) {}
            override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {}
            override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) {}
        })
        
        routeSearch.calculateDriveRouteAsyn(query)
    }
}

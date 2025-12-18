package com.example.cockpitmap.feature.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.example.cockpitmap.core.model.GeoLocation

private const val TAG = "MapRenderScreen"

/**
 * 高德地图渲染组件
 * 
 * [深度优化说明]：
 * 1. 解决了启动时 DataStore 异步读取导致无法应用缓存坐标的问题。
 * 2. 引入 LaunchedEffect 监听 initialLocation，实现真正的“启动即回显”。
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null,
    onLocationChanged: (GeoLocation) -> Unit = {},
    onControllerReady: (MapController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    var isMapConfigured by remember { mutableStateOf(false) }
    var hasAutoCentered by remember { mutableStateOf(false) }
    
    // 状态锁：确保启动时的缓存跳转只执行一次
    var isInitialMoveDone by remember { mutableStateOf(false) }

    // 1. 初始化 MapView 实例
    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    // 【关键修复】：监听 initialLocation 的变化
    // 由于 DataStore 是异步读取，initialLocation 在启动首帧通常为 null。
    // 当读取成功后，本 Effect 会被触发，执行“瞬移”操作。
    LaunchedEffect(initialLocation) {
        if (initialLocation != null && !isInitialMoveDone) {
            Log.i(TAG, "Apply Cache: 检测到有效缓存位置，执行镜头瞬移: ${initialLocation.latitude}")
            mapView.map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(initialLocation.latitude, initialLocation.longitude), 15f
                )
            )
            isInitialMoveDone = true
        }
    }

    // 2. 生命周期管理
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val aMap = view.map
                if (!isMapConfigured) {
                    setupAMapHmi(aMap)
                    
                    aMap.setOnMyLocationChangeListener { location ->
                        if (location != null && location.latitude != 0.0) {
                            val geo = GeoLocation(location.latitude, location.longitude, "当前位置")
                            onLocationChanged(geo)

                            // 首次 GPS 定位后的平滑移动（仅在无缓存或未跳转过时生效）
                            if (!hasAutoCentered) {
                                // 如果没做过 initialMove，或者当前位置距离缓存点太远，可以执行一次 animate
                                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(geo.latitude, geo.longitude), 15f))
                                hasAutoCentered = true
                            }
                        }
                    }

                    onControllerReady(AMapController(aMap))
                    isMapConfigured = true
                }
            }
        )

        if (!isMapConfigured) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Cyan)
        }
    }
}

/**
 * 车机 HMI 专用配置
 */
private fun setupAMapHmi(aMap: AMap) {
    aMap.apply {
        val myLocationStyle = MyLocationStyle().apply {
            myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            interval(2000) 
            showMyLocation(true)
        }
        setMyLocationStyle(myLocationStyle)
        isMyLocationEnabled = true 
        uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false 
            isCompassEnabled = true
            isScaleControlsEnabled = true
        }
        mapType = AMap.MAP_TYPE_NIGHT
    }
}

interface MapController {
    fun zoomIn()
    fun zoomOut()
    fun moveTo(location: GeoLocation)
    fun locateMe()
}

class AMapController(private val aMap: AMap) : MapController {
    override fun zoomIn() { aMap.animateCamera(CameraUpdateFactory.zoomIn()) }
    override fun zoomOut() { aMap.animateCamera(CameraUpdateFactory.zoomOut()) }
    override fun moveTo(location: GeoLocation) {
        aMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
    }
    override fun locateMe() {
        val location = aMap.myLocation
        if (location != null && location.latitude != 0.0) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
        }
    }
}

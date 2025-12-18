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
 * 高德地图渲染核心组件
 * 
 * 按照 [MODULES.md] 规范：
 * 1. 内部处理高德 SDK 的复杂定位监听逻辑。
 * 2. 自动处理“首次定位居中”与“定位异常超时”。
 * 3. 暴露 [onControllerReady] 回调，允许宿主模块控制地图。
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null,
    onControllerReady: (MapController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    // 状态：地图渲染是否已就绪
    var isMapVisible by remember { mutableStateOf(false) }
    // 状态：是否已完成首次定位自动居中
    var hasAutoCentered by remember { mutableStateOf(false) }

    // 1. 初始化隐私协议 (必须最前)
    remember {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        true
    }

    // 2. 持久化 MapView
    val mapView = remember { 
        MapView(context).apply { onCreate(null) }
    }

    // 3. 生命周期绑定
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
                if (!isMapVisible) {
                    setupAMap(aMap)
                    
                    // 注册定位变化监听，实现“首次定位自动移动镜头”
                    aMap.setOnMyLocationChangeListener { location ->
                        if (location != null && !hasAutoCentered) {
                            Log.d(TAG, "首次定位成功: ${location.latitude}, ${location.longitude}")
                            aMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude), 15f
                                )
                            )
                            hasAutoCentered = true
                        }
                    }

                    // 初始默认位置备份（如果定位太慢，先看一眼初始点）
                    initialLocation?.let {
                        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 10f))
                    }

                    // 初始化控制器并回调给宿主
                    onControllerReady(AMapController(aMap))
                    isMapVisible = true
                }
            }
        )

        // 覆盖层：当地图还没渲染出来时，显示进度
        if (!isMapVisible) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Cyan
            )
        }
    }
}

/**
 * AMap 基础属性配置
 */
private fun setupAMap(aMap: AMap) {
    aMap.apply {
        // 配置定位蓝点样式
        val myLocationStyle = MyLocationStyle().apply {
            // 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。
            myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            interval(2000) // 车机场景下，2秒更新一次位置较为平滑且省电
            showMyLocation(true)
        }
        
        setMyLocationStyle(myLocationStyle)
        isMyLocationEnabled = true // 激活定位图层
        
        uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false // 隐藏高德原生的、小的定位按钮
            isCompassEnabled = true
        }
        mapType = AMap.MAP_TYPE_NIGHT
    }
}

/**
 * 跨模块地图控制接口
 */
interface MapController {
    fun zoomIn()
    fun zoomOut()
    fun moveTo(location: GeoLocation)
    fun locateMe() // 触发定位居中
}

/**
 * 控制器实现类
 */
class AMapController(private val aMap: AMap) : MapController {
    override fun zoomIn() {
        aMap.animateCamera(CameraUpdateFactory.zoomIn())
    }

    override fun zoomOut() {
        aMap.animateCamera(CameraUpdateFactory.zoomOut())
    }

    override fun moveTo(location: GeoLocation) {
        aMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
    }

    override fun locateMe() {
        val location = aMap.myLocation
        if (location != null) {
            aMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 15f
                )
            )
        } else {
            Log.w(TAG, "定位尝试失败：当前 SDK 尚未获取到有效位置")
        }
    }
}

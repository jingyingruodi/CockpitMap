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
 * [修复说明]：针对黑屏与定位失效的深度优化。
 * 1. 强制在 MapView 实例化前同步执行隐私合规确认。
 * 2. 增强了 AndroidView 的首次加载状态管理。
 * 3. 严格同步 Native 生命周期，防止车机端内存泄漏。
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null,
    onControllerReady: (MapController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    // 地图是否已成功可见的状态
    var isMapVisible by remember { mutableStateOf(false) }
    // 首次定位是否已完成自动居中
    var hasAutoCentered by remember { mutableStateOf(false) }

    // 【核心修复】：隐私合规确认必须在 MapView 创建之前。
    // 在 remember 块内按顺序执行，确保时序正确。
    val mapView = remember { 
        Log.d(TAG, "执行隐私合规初始化...")
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        
        Log.d(TAG, "创建 MapView 实例...")
        MapView(context).apply {
            // 同步调用 onCreate 是激活渲染引擎的关键
            onCreate(null) 
        }
    }

    // 将 AMap 的 Native 生命周期与 Compose 宿主生命周期严格绑定
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d(TAG, "AMap: ON_RESUME")
                    mapView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(TAG, "AMap: ON_PAUSE")
                    mapView.onPause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(TAG, "AMap: ON_DESTROY")
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val aMap = view.map
                
                // 仅在首次渲染时执行地图配置
                if (!isMapVisible) {
                    Log.d(TAG, "开始配置 AMap 属性...")
                    setupAMap(aMap)
                    
                    // 注册定位变化监听
                    aMap.setOnMyLocationChangeListener { location ->
                        if (location != null && location.latitude != 0.0) {
                            if (!hasAutoCentered) {
                                Log.i(TAG, "捕获到首次有效定位: ${location.latitude}, ${location.longitude}")
                                aMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(location.latitude, location.longitude), 15f
                                    )
                                )
                                hasAutoCentered = true
                            }
                        } else {
                            Log.w(TAG, "收到无效定位数据 (0,0) 或 null，等待信号锁定...")
                        }
                    }

                    // 初始位置展示逻辑
                    initialLocation?.let {
                        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 10f))
                    }

                    // 回调控制器给宿主模块
                    onControllerReady(AMapController(aMap))
                    isMapVisible = true
                }
            }
        )

        // 如果地图尚未渲染完成，显示科技感的进度条
        if (!isMapVisible) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Cyan
            )
        }
    }
}

/**
 * 车机 HMI 专项地图属性配置
 */
private fun setupAMap(aMap: AMap) {
    aMap.apply {
        val myLocationStyle = MyLocationStyle().apply {
            // 模式说明：连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。
            myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            interval(2000) 
            showMyLocation(true)
        }
        
        setMyLocationStyle(myLocationStyle)
        // 显式激活定位图层
        isMyLocationEnabled = true 
        
        uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false 
            isCompassEnabled = true
            isScaleControlsEnabled = true
        }
        // 车载适配：强制夜间模式
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
    fun locateMe()
}

/**
 * 控制器具体实现
 */
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
        } else {
            Log.e(TAG, "手动定位失败：myLocation 为空或坐标仍为 (0,0)。请检查 GPS 信号或权限。")
        }
    }
}

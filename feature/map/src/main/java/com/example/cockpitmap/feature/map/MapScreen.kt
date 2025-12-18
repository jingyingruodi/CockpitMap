package com.example.cockpitmap.feature.map

import android.content.Context
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
import com.example.cockpitmap.core.model.GeoLocation

/**
 * 高德地图渲染核心组件
 * 
 * 按照 [MODULES.md] 规范：
 * 1. 封装第三方 SDK 细节，对外仅暴露基础配置。
 * 2. 严格管理 native 视图生命周期，防止车机长时间运行内存溢出。
 * 3. 针对“黑屏”问题：优化了隐私协议初始化时序，确保在视图创建前完成合规确认。
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    // 状态管理：地图是否已完成初始化
    var isMapInitialized by remember { mutableStateOf(false) }

    // 【关键修复】：高德 SDK 隐私协议必须在 MapView 实例化 *之前* 调用
    // 使用 SideEffect 或在 remember 块之前确保执行，防止黑屏或 SDK 拦截渲染
    remember {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        true
    }

    // 实例化 MapView 并手动调用其初始声明周期
    val mapView = remember { 
        MapView(context).apply {
            // 某些情况下，factory 运行太快，在这里先同步一次核心生命周期
            onCreate(null) 
        }
    }

    // 将 AMap 全生命周期与 Compose 宿主严格绑定
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
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
        // 渲染底层的原生地图视图
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // 地图加载完成后的首次配置
                if (!isMapInitialized) {
                    setupAMap(view.map)
                    initialLocation?.let {
                        view.map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude), 
                                12f
                            )
                        )
                    }
                    isMapInitialized = true
                }
            }
        )

        // 如果地图还没准备好，显示一个优雅的加载进度条，避免纯黑屏闪烁
        if (!isMapInitialized) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Cyan
            )
        }
    }
}

/**
 * 高德地图 HMI 专项配置
 * 
 * 适配守则：
 * 1. 强制夜间模式：减少车内光污染。
 * 2. 简化 UI：禁用原生缩放按钮，由 feature 模块自定义大尺寸按钮接管。
 */
private fun setupAMap(aMap: AMap) {
    aMap.apply {
        uiSettings.apply {
            isZoomControlsEnabled = false 
            isCompassEnabled = true      
            isScaleControlsEnabled = true 
            isMyLocationButtonEnabled = false // 禁用原生定位按钮，后续自定义
        }
        // 设置地图渲染类型为夜间/科技感模式
        mapType = AMap.MAP_TYPE_NIGHT
    }
}

/**
 * 地图交互控制器实现类
 */
class AMapController(private val aMap: AMap) : MapController {
    override fun zoomIn() {
        aMap.animateCamera(CameraUpdateFactory.zoomIn())
    }

    override fun zoomOut() {
        aMap.animateCamera(CameraUpdateFactory.zoomOut())
    }

    override fun moveTo(location: GeoLocation) {
        aMap.animateCamera(
            CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude))
        )
    }
}

/**
 * 跨模块地图控制抽象接口
 * 定义在 [:feature:map]，由 [app] 或其他业务模块调用
 */
interface MapController {
    fun zoomIn()
    fun zoomOut()
    fun moveTo(location: GeoLocation)
}

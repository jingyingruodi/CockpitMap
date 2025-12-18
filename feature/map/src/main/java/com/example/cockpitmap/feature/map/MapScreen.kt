package com.example.cockpitmap.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
 * 高德地图渲染屏幕。
 * 封装了高德 SDK 的 MapView，并处理了 Compose 生命周期。
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null
) {
    val context = LocalContext.current
    
    // 1. 高德地图隐私合规检查 (必须在 MapView 创建前调用)
    LaunchedEffect(Unit) {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
    }

    // 2. 持久化 MapView 对象，处理生命周期
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // 3. 将 AMap 生命周期与 Compose 生命周期绑定
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // 4. 渲染地图 View
    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    ) { view ->
        val aMap = view.map
        
        // 5. 地图初始化配置
        setupAMap(aMap)
        
        // 6. 处理初始位置
        initialLocation?.let {
            aMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude), 
                    15f // 默认缩放级别
                )
            )
        }
    }
}

/**
 * 高德地图的具体配置（例如：夜间模式、手势控制等）
 */
private fun setupAMap(aMap: AMap) {
    aMap.apply {
        uiSettings.apply {
            isZoomControlsEnabled = false // 禁用自带缩放按钮，使用我们自定义的 UI
            isCompassEnabled = true      // 显示指南针
            isScaleControlsEnabled = true // 显示比例尺
        }
        // 设置为夜间模式，更符合车机审美
        mapType = AMap.MAP_TYPE_NIGHT
    }
}

/**
 * 实现地图控制器接口
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

interface MapController {
    fun zoomIn()
    fun zoomOut()
    fun moveTo(location: GeoLocation)
}

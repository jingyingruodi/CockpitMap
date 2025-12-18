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
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.example.cockpitmap.core.model.GeoLocation

private const val TAG = "MapRenderScreen"

/**
 * 高德地图渲染核心组件。
 * 
 * [功能增强 - 影子定位]：
 * 1. 影子图标 (Ghost Marker)：在 GPS 信号锁定前，利用缓存坐标在地图上显示一个半透明锚点。
 * 2. 自动销毁逻辑：一旦真实定位触发，影子图标立即消失，确保视觉逻辑一致。
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
    var isInitialMoveDone by remember { mutableStateOf(false) }

    // 持有影子标记点的引用，以便在定位成功后移除它
    val ghostMarkerState = remember { mutableStateOf<Marker?>(null) }

    // 实例化 MapView
    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    // 【影子图标逻辑】：启动后，只要 initialLocation (缓存) 加载成功，立即打上标记
    LaunchedEffect(initialLocation) {
        if (initialLocation != null && !isInitialMoveDone) {
            val aMap = mapView.map
            val latLng = LatLng(initialLocation.latitude, initialLocation.longitude)
            
            // 1. 镜头跳转
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            
            // 2. 放置影子标记点
            ghostMarkerState.value?.remove() // 防重
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .anchor(0.5f, 0.5f)
                    .alpha(0.6f) // 半透明
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("记忆位置")
            )
            ghostMarkerState.value = marker
            isInitialMoveDone = true
            Log.d(TAG, "Ghost Marker 已部署 at: ${initialLocation.latitude}")
        }
    }

    // 生命周期联动
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    ghostMarkerState.value?.remove()
                    mapView.onDestroy()
                }
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
                    
                    // 定位监听：这是影子图标退场的触发器
                    aMap.setOnMyLocationChangeListener { location ->
                        if (location != null && location.latitude != 0.0) {
                            
                            // 【核心联动】：真实定位成功，移除影子
                            ghostMarkerState.value?.let {
                                Log.i(TAG, "真实 GPS 锁定，正在移除影子锚点...")
                                it.remove()
                                ghostMarkerState.value = null
                            }

                            val geo = GeoLocation(location.latitude, location.longitude, "当前位置")
                            onLocationChanged(geo)

                            if (!hasAutoCentered) {
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

package com.example.cockpitmap.feature.map

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 高德地图渲染核心。
 * 
 * [功能微调]：
 * 1. 影子图标校验：增加了对 0.0 坐标的过滤，防止卸载重装后的图标闪现。
 * 2. 状态提示全覆盖：无论是否有缓存，在 GPS 锁定前均显示定位提示。
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
    
    // GPS 锁定状态
    var isGpsLocked by remember { mutableStateOf(false) }

    // 持有影子标记引用
    val ghostMarkerState = remember { mutableStateOf<Marker?>(null) }

    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    // 缓存位置应用逻辑
    LaunchedEffect(initialLocation) {
        // 【关键修复】：增加 latitude != 0.0 校验，防止卸载重装后读取到 DataStore 初始零值导致的图标闪现
        if (initialLocation != null && initialLocation.latitude != 0.0 && !isInitialMoveDone) {
            val aMap = mapView.map
            val latLng = LatLng(initialLocation.latitude, initialLocation.longitude)
            
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            
            // 部署影子图标
            ghostMarkerState.value?.remove()
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .anchor(0.5f, 0.5f)
                    .alpha(0.6f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("记忆位置")
            )
            ghostMarkerState.value = marker
            isInitialMoveDone = true
        }
    }

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
                    
                    aMap.setOnMyLocationChangeListener { location ->
                        if (location != null && location.latitude != 0.0) {
                            // 卫星锁定成功
                            isGpsLocked = true
                            
                            // 销毁影子
                            ghostMarkerState.value?.let {
                                Log.i(TAG, "GPS Fix: 移除影子锚点")
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

        // --- 搜星提示条 ---
        // 【修改逻辑】：只要 GPS 未锁定，且地图已基本就绪，就显示提示（覆盖无缓存启动场景）
        AnimatedVisibility(
            visible = !isGpsLocked && isMapConfigured,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
        ) {
            GpsStatusInfoCard()
        }

        if (!isMapConfigured) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Cyan)
        }
    }
}

@Composable
private fun GpsStatusInfoCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Cyan)
            Spacer(Modifier.width(12.dp))
            Text(text = "正在获取卫星定位...", color = Color.White, fontSize = 14.sp)
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

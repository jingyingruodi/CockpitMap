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
 * 高德内置地图样式映射枚举
 */
enum class CustomMapStyle(val type: Int) {
    NORMAL(AMap.MAP_TYPE_NORMAL),   // 标准白天模式
    NIGHT(AMap.MAP_TYPE_NIGHT),     // 护眼黑夜模式 (车机推荐)
    SATELLITE(AMap.MAP_TYPE_SATELLITE), // 卫星实景模式
    NAVI(AMap.MAP_TYPE_NAVI)        // 纯净导航模式
}

/**
 * [MapRenderScreen] 地图渲染核心组件。
 * 
 * 职责：
 * 1. 管理 AMap SDK 的生命周期同步。
 * 2. 调度“影子定位”逻辑，在搜星成功前展示缓存位置。
 * 3. 响应外部控制句柄 [MapController]。
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
    
    // 渲染状态控制
    var isMapConfigured by remember { mutableStateOf(false) }
    var hasAutoCentered by remember { mutableStateOf(false) }
    var isInitialMoveDone by remember { mutableStateOf(false) }
    var isGpsLocked by remember { mutableStateOf(false) }

    // 持有启动时的影子标记点 (Marker)，以便在真实定位后精准销毁
    val ghostMarkerState = remember { mutableStateOf<Marker?>(null) }

    // 初始化 MapView 实例，并与 remember 绑定，确保仅创建一次
    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    // [启动回显效应]：监听 DataStore 异步加载的数据
    LaunchedEffect(initialLocation) {
        // 增加 latitude != 0.0 严苛校验，防止空数据导致的影子点闪现
        if (initialLocation != null && initialLocation.latitude != 0.0 && !isInitialMoveDone) {
            val aMap = mapView.map
            val latLng = LatLng(initialLocation.latitude, initialLocation.longitude)
            
            // 1. 瞬间移动镜头至上次位置
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            
            // 2. 部署影子图标：Azure 蓝色半透明样式
            ghostMarkerState.value?.remove() 
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .anchor(0.5f, 0.5f)
                    .alpha(0.6f) 
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("上次记忆位置")
            )
            ghostMarkerState.value = marker
            isInitialMoveDone = true
        }
    }

    // [生命周期管理]：确保 AMap 资源随系统生命周期正确回收，防止内存泄漏
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
        // Compose 互操作层：将 Android View (MapView) 嵌入声明式 UI
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val aMap = view.map
                if (!isMapConfigured) {
                    setupAMapHmi(aMap)
                    
                    // 定位监听：这是影子点退场、真实蓝点入场的唯一触发点
                    aMap.setOnMyLocationChangeListener { location ->
                        if (location != null && location.latitude != 0.0) {
                            isGpsLocked = true
                            
                            // 卫星锁定成功，销毁影子点
                            ghostMarkerState.value?.let {
                                it.remove()
                                ghostMarkerState.value = null
                            }

                            val geo = GeoLocation(location.latitude, location.longitude, "当前位置")
                            onLocationChanged(geo)

                            // 首次平滑居中逻辑
                            if (!hasAutoCentered) {
                                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(geo.latitude, geo.longitude), 15f))
                                hasAutoCentered = true
                            }
                        }
                    }

                    // 对外暴露控制 handle
                    onControllerReady(AMapController(aMap))
                    isMapConfigured = true
                }
            }
        )

        // [搜星交互]：只要卫星未锁定，顶部即展示悬浮提示条
        AnimatedVisibility(
            visible = !isGpsLocked && isMapConfigured,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
        ) {
            GpsStatusInfoCard()
        }

        // 启动加载环
        if (!isMapConfigured) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Cyan)
        }
    }
}

/**
 * 搜星状态展示卡片。
 */
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

/**
 * 统一配置车机专属样式。
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
            isZoomControlsEnabled = false // 禁用原生按钮，改用我们的 Compose 悬浮按钮
            isMyLocationButtonEnabled = false 
            isCompassEnabled = true
        }
        mapType = AMap.MAP_TYPE_NIGHT
    }
}

/**
 * 地图控制接口。
 */
interface MapController {
    fun zoomIn()
    fun zoomOut()
    fun moveTo(location: GeoLocation)
    fun locateMe()
    fun setMapStyle(style: CustomMapStyle)
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
    override fun setMapStyle(style: CustomMapStyle) { aMap.mapType = style.type }
}

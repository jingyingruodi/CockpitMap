package com.example.cockpitmap.feature.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.location.AMapLocation
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.*
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.MapController
import com.example.cockpitmap.core.model.RouteInfo

/**
 * 车载地图渲染核心组件。
 */
@Composable
fun MapRenderScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null,
    onLocationChanged: (GeoLocation) -> Unit = {},
    onControllerReady: (MapController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    val isLocationLocked by viewModel.isLocationLocked.collectAsStateWithLifecycle()
    var hasInitialAutoCenter by remember { mutableStateOf(false) } 

    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    // 初始化位置处理
    LaunchedEffect(initialLocation) {
        if (initialLocation != null && !hasInitialAutoCenter) {
            mapView.map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(initialLocation.latitude, initialLocation.longitude), 
                    15f
                )
            )
            hasInitialAutoCenter = true
        }
    }

    DisposableEffect(lifecycle) {
        val aMap = mapView.map
        
        val customSource = object : LocationSource {
            private var amapListener: LocationSource.OnLocationChangedListener? = null
            override fun activate(p0: LocationSource.OnLocationChangedListener?) { amapListener = p0 }
            override fun deactivate() { amapListener = null }
            fun push(loc: Location) {
                val amapLoc = AMapLocation(loc).apply {
                    bearing = loc.bearing
                    accuracy = loc.accuracy
                }
                amapListener?.onLocationChanged(amapLoc)
            }
        }

        val nativeManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val nativeListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.latitude != 0.0) {
                    val geo = GeoLocation(location.latitude, location.longitude, "系统位置")
                    viewModel.updateLocation(geo)
                    customSource.push(location)
                    onLocationChanged(geo)
                    
                    if (!hasInitialAutoCenter) {
                        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(geo.latitude, geo.longitude), 15f))
                        hasInitialAutoCenter = true
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
            override fun onProviderEnabled(p0: String) {}
            override fun onProviderDisabled(p0: String) {}
        }

        setupNativeTracking(nativeManager, nativeListener)
        aMap.setLocationSource(customSource)
        aMap.setMyLocationEnabled(true)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    nativeManager.removeUpdates(nativeListener)
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { 
            lifecycle.removeObserver(observer)
            nativeManager.removeUpdates(nativeListener)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                setupMapStyles(view.map)
                onControllerReady(AMapController(view.map))
            }
        )

        AnimatedVisibility(
            visible = !isLocationLocked,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
        ) {
            CockpitSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(text = "多源定位启动中...", fontSize = 14.sp)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun setupNativeTracking(manager: LocationManager, listener: LocationListener) {
    try {
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, listener)
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener)
    } catch (e: Exception) {
        Log.e("MapScreen", "Native track failed: ${e.message}")
    }
}

private fun setupMapStyles(aMap: AMap) {
    val style = MyLocationStyle().apply {
        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        showMyLocation(true)
        strokeColor(AndroidColor.TRANSPARENT)
        radiusFillColor(AndroidColor.TRANSPARENT)
    }
    aMap.setMyLocationStyle(style)
    aMap.getUiSettings().setZoomControlsEnabled(false)
    aMap.getUiSettings().setCompassEnabled(true)
    // 默认保持为普通模式
}

/**
 * [MapController] 实现类。
 */
class AMapController(
    private val aMap: AMap
) : MapController {
    
    private var currentSearchMarker: Marker? = null
    private var currentPolyline: Polyline? = null
    private var onMarkerLongClickListener: ((GeoLocation) -> Unit)? = null

    init {
        aMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
        
        aMap.setOnMapLongClickListener { latLng ->
            currentSearchMarker?.let { marker ->
                val markerLatLng = marker.position
                val results = FloatArray(1)
                Location.distanceBetween(
                    latLng.latitude, latLng.longitude,
                    markerLatLng.latitude, markerLatLng.longitude,
                    results
                )
                if (results[0] < 100) {
                    onMarkerLongClickListener?.invoke(
                        GeoLocation(markerLatLng.latitude, markerLatLng.longitude, marker.title ?: "")
                    )
                }
            }
        }
    }

    override fun zoomIn() { aMap.animateCamera(CameraUpdateFactory.zoomIn()) }
    override fun zoomOut() { aMap.animateCamera(CameraUpdateFactory.zoomOut()) }
    
    override fun moveTo(location: GeoLocation) {
        if (location.latitude != 0.0) {
            aMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
        }
    }

    override fun showMarker(location: GeoLocation) {
        currentSearchMarker?.remove()
        val markerOptions = MarkerOptions().apply {
            position(LatLng(location.latitude, location.longitude))
            title(location.name)
            icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            draggable(false)
        }
        currentSearchMarker = aMap.addMarker(markerOptions)
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
    }

    override fun clearMarkers() {
        currentSearchMarker?.remove()
        currentSearchMarker = null
    }

    override fun drawRoute(route: RouteInfo) {
        currentPolyline?.remove()
        if (route.polyline.isEmpty()) return

        val points = route.polyline.map { LatLng(it.latitude, it.longitude) }
        val options = PolylineOptions().apply {
            addAll(points)
            width(20f)
            color(AndroidColor.parseColor("#3498db"))
            useGradient(true)
        }
        currentPolyline = aMap.addPolyline(options)
        
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
    }

    override fun clearRoute() {
        currentPolyline?.remove()
        currentPolyline = null
    }

    override fun locateMe() {
        val loc = aMap.myLocation
        if (loc != null && loc.latitude != 0.0) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
        }
    }
    
    override fun setMapStyle(type: Int) { aMap.setMapType(type) }

    override fun setOnMarkerLongClickListener(onLongClick: (GeoLocation) -> Unit) {
        this.onMarkerLongClickListener = onLongClick
    }

    override fun setFollowingMode(enabled: Boolean) {
        if (enabled) {
            // 1. 切换为导航专用样式 (MAP_TYPE_NAVI)
            aMap.setMapType(AMap.MAP_TYPE_NAVI)
            
            // 2. 配置强锁定跟随模式
            val style = MyLocationStyle().apply {
                // LOCATION_TYPE_MAP_ROTATE: 连续定位、蓝点带动地图移动，且地图始终随车头旋转
                myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE)
                showMyLocation(true)
                strokeColor(AndroidColor.TRANSPARENT)
                radiusFillColor(AndroidColor.TRANSPARENT)
                // 导航蓝点间隔 1s 更新位置，保证平滑性
                interval(1000)
            }
            aMap.setMyLocationStyle(style)
            
            // 3. 立即将相机对齐到当前坐标，并设置 3D 视角
            val loc = aMap.myLocation
            if (loc != null && loc.latitude != 0.0) {
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder()
                        .target(LatLng(loc.latitude, loc.longitude))
                        .zoom(18f)   // 导航缩放级别
                        .tilt(45f)   // 倾斜视角
                        .bearing(loc.bearing) // 初始朝向同步
                        .build()
                )
                aMap.animateCamera(cameraUpdate)
            }
        } else {
            // 1. 恢复普通地图样式
            aMap.setMapType(AMap.MAP_TYPE_NORMAL)
            
            // 2. 恢复普通定位模式 (LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER: 蓝点转地图不转)
            val style = MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                showMyLocation(true)
                strokeColor(AndroidColor.TRANSPARENT)
                radiusFillColor(AndroidColor.TRANSPARENT)
            }
            aMap.setMyLocationStyle(style)
            
            // 3. 恢复平视视角
            aMap.animateCamera(CameraUpdateFactory.changeTilt(0f))
        }
    }
}

package com.example.cockpitmap.feature.map

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.*
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.MapController

/**
 * [车载级混合定位引擎 - 原生视觉增强版]
 * 
 * 核心逻辑：
 * 1. 彻底由原生 LocationManager 驱动高德地图蓝点绘制。
 * 2. 即使无 Key 也能显示位置图标、精度圈和车头方向。
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
    
    var isLocationLocked by remember { mutableStateOf(false) }
    var hasInitialAutoCenter by remember { mutableStateOf(false) } 

    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycle) {
        val aMap = mapView.map
        
        // 核心：自定义原生定位驱动源
        val customSource = object : LocationSource {
            private var amapListener: LocationSource.OnLocationChangedListener? = null
            
            override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                amapListener = p0
            }

            override fun deactivate() {
                amapListener = null
            }

            // 对外暴露推送接口
            fun push(loc: Location) {
                val amapLoc = AMapLocation(loc).apply {
                    // 补全所有视觉要素
                    bearing = loc.bearing
                    accuracy = loc.accuracy
                    altitude = loc.altitude
                }
                amapListener?.onLocationChanged(amapLoc)
            }
        }

        val nativeManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val nativeListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.latitude != 0.0) {
                    isLocationLocked = true
                    // 驱动地图蓝点亮起与旋转
                    customSource.push(location)
                    
                    processLocationUpdate(location, aMap, "系统", hasInitialAutoCenter, onLocationChanged) {
                        hasInitialAutoCenter = true
                    }
                }
            }
            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
            override fun onProviderEnabled(p0: String) {}
            override fun onProviderDisabled(p0: String) {}
        }

        // 高德 SDK 仍作为静默精修器（如果 Key 有效）
        val amapProvider = HybridLocationProvider(context) { location ->
            isLocationLocked = true
            customSource.push(location)
            processLocationUpdate(location, aMap, "精修", hasInitialAutoCenter, onLocationChanged) {
                hasInitialAutoCenter = true
            }
        }
        
        setupNativeTracking(nativeManager, nativeListener)
        aMap.setLocationSource(customSource) // 统一使用我们的自定义驱动源
        aMap.isMyLocationEnabled = true

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    nativeManager.removeUpdates(nativeListener)
                    amapProvider.deactivate()
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { 
            lifecycle.removeObserver(observer)
            nativeManager.removeUpdates(nativeListener)
            amapProvider.deactivate()
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
                    Text(text = "定位启动中...", fontSize = 14.sp)
                }
            }
        }
    }
}

private fun processLocationUpdate(
    location: Location,
    aMap: AMap,
    source: String,
    autoCenterDone: Boolean,
    onChanged: (GeoLocation) -> Unit,
    onCenterSuccess: () -> Unit
) {
    onChanged(GeoLocation(location.latitude, location.longitude, "位置($source)"))
    if (!autoCenterDone) {
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(location.latitude, location.longitude), 15f
        ))
        onCenterSuccess()
    }
}

@SuppressLint("MissingPermission")
private fun setupNativeTracking(manager: LocationManager, listener: LocationListener) {
    try {
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, listener)
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener)
    } catch (e: Exception) {
        Log.e("HybridLoc", "Native tracking failed: ${e.message}")
    }
}

class HybridLocationProvider(
    private val context: Context,
    private val onAmapUpdate: (Location) -> Unit
) {
    private var amapClient: AMapLocationClient? = null

    init {
        try {
            amapClient = AMapLocationClient(context).apply {
                setLocationListener { amapLocation ->
                    if (amapLocation != null && amapLocation.errorCode == 0 && amapLocation.latitude != 0.0) {
                        onAmapUpdate(amapLocation)
                    }
                }
                val options = AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    interval = 2000
                }
                setLocationOption(options)
                startLocation()
            }
        } catch (e: Exception) {}
    }

    fun deactivate() {
        amapClient?.stopLocation()
        amapClient?.onDestroy()
        amapClient = null
    }
}

private fun setupMapStyles(aMap: AMap) {
    val style = MyLocationStyle().apply {
        // [核心优化]：使用带有方向旋转的定位图标模式
        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        showMyLocation(true)
        strokeColor(Color.Transparent.hashCode())
        radiusFillColor(Color.Transparent.hashCode())
    }
    aMap.setMyLocationStyle(style)
    aMap.uiSettings.isZoomControlsEnabled = false
    aMap.uiSettings.isCompassEnabled = true
    aMap.mapType = AMap.MAP_TYPE_NIGHT
}

class AMapController(private val aMap: AMap) : MapController {
    override fun zoomIn() { aMap.animateCamera(CameraUpdateFactory.zoomIn()) }
    override fun zoomOut() { aMap.animateCamera(CameraUpdateFactory.zoomOut()) }
    override fun moveTo(location: GeoLocation) {
        if (location.latitude != 0.0) {
            aMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
        }
    }
    override fun locateMe() {
        val loc = aMap.myLocation
        if (loc != null && loc.latitude != 0.0) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
        }
    }
    override fun setMapStyle(type: Int) { aMap.mapType = type }
}

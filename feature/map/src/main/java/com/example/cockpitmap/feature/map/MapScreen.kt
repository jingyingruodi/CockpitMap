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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
 * [HMI 地图渲染组件 - 重构标准版]
 * 
 * 职责：
 * 1. 负责高德地图 SDK 的生命周期管理与渲染。
 * 2. 实现 [MapController] 接口暴露给外部。
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
            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
            override fun onProviderEnabled(p0: String) {}
            override fun onProviderDisabled(p0: String) {}
        }

        setupNativeTracking(nativeManager, nativeListener)
        aMap.setLocationSource(customSource)
        aMap.isMyLocationEnabled = true

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
        strokeColor(Color.Transparent.hashCode())
        radiusFillColor(Color.Transparent.hashCode())
    }
    aMap.setMyLocationStyle(style)
    aMap.uiSettings.isZoomControlsEnabled = false
    aMap.uiSettings.isCompassEnabled = true
    // 默认修改为标准模式
    aMap.mapType = AMap.MAP_TYPE_NORMAL
}

/**
 * 高德地图控制器实现类。
 */
class AMapController(private val aMap: AMap) : MapController {
    
    private var currentSearchMarker: Marker? = null
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

    override fun locateMe() {
        val loc = aMap.myLocation
        if (loc != null && loc.latitude != 0.0) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
        }
    }
    
    override fun setMapStyle(type: Int) { aMap.mapType = type }

    override fun setOnMarkerLongClickListener(onLongClick: (GeoLocation) -> Unit) {
        this.onMarkerLongClickListener = onLongClick
    }
}

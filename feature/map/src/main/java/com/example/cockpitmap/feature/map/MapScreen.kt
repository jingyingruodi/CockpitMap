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
    aMap.setMapType(AMap.MAP_TYPE_NORMAL)
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
            // [核心修复]：强制切换为位置、视角双跟随模式
            val style = MyLocationStyle().apply {
                // LOCATION_TYPE_MAP_ROTATE 会强制地图中心保持在自车坐标，且随车头旋转
                myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE)
                showMyLocation(true)
                strokeColor(AndroidColor.TRANSPARENT)
                radiusFillColor(AndroidColor.TRANSPARENT)
            }
            aMap.setMyLocationStyle(style)
            
            // 立即对齐当前位置
            val loc = aMap.myLocation
            if (loc != null && loc.latitude != 0.0) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 18f))
            }
            aMap.animateCamera(CameraUpdateFactory.changeTilt(45f)) // 设置导航视角倾斜
        } else {
            // 回到普通预览模式
            val style = MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                showMyLocation(true)
                strokeColor(AndroidColor.TRANSPARENT)
                radiusFillColor(AndroidColor.TRANSPARENT)
            }
            aMap.setMyLocationStyle(style)
            aMap.animateCamera(CameraUpdateFactory.changeTilt(0f)) // 恢复平视
        }
    }
}

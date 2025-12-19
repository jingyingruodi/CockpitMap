package com.example.cockpitmap.feature.map

import android.content.Context
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
 * 高德地图渲染核心组件。
 * 
 * [优化修复记录]：
 * 1. 物理级彻底移除所有非法占位符。
 * 2. 采用 LocationSource 接管地图定位源，提升接入速度。
 * 3. hasInitialAutoCenter 确保首次定位成功后【仅执行一次】居中跳转。
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
    var isGpsLocked by remember { mutableStateOf(false) }
    var hasInitialAutoCenter by remember { mutableStateOf(false) } 
    val ghostMarkerState = remember { mutableStateOf<Marker?>(null) }

    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycle) {
        val aMap = mapView.map
        val locationProvider = AMapLocationProvider(context) { location ->
            if (location.latitude != 0.0 && location.longitude != 0.0) {
                isGpsLocked = true
                ghostMarkerState.value?.remove()
                onLocationChanged(GeoLocation(location.latitude, location.longitude, "当前位置"))
                
                // 定位成功后执行唯一一次的主动居中，随后释放控制权
                if (!hasInitialAutoCenter) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 15f
                    ))
                    hasInitialAutoCenter = true
                }
            }
        }
        
        aMap.setLocationSource(locationProvider)
        aMap.isMyLocationEnabled = true

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    ghostMarkerState.value?.remove()
                    locationProvider.deactivate()
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { 
            lifecycle.removeObserver(observer)
            locationProvider.deactivate()
        }
    }

    LaunchedEffect(initialLocation) {
        if (initialLocation != null && initialLocation.latitude != 0.0 && !isGpsLocked) {
            val latLng = LatLng(initialLocation.latitude, initialLocation.longitude)
            mapView.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            ghostMarkerState.value?.remove()
            ghostMarkerState.value = mapView.map.addMarker(
                MarkerOptions().position(latLng).anchor(0.5f, 0.5f).alpha(0.6f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (!isMapConfigured) {
                    setupMapHmi(view.map)
                    onControllerReady(AMapController(view.map))
                    isMapConfigured = true
                }
            }
        )

        AnimatedVisibility(
            visible = !isGpsLocked && isMapConfigured,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
        ) {
            CockpitSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(text = "定位引擎接入中...", fontSize = 14.sp)
                }
            }
        }
    }
}

class AMapLocationProvider(
    private val context: Context,
    private val onLocationReady: (AMapLocation) -> Unit
) : LocationSource, AMapLocationListener {
    private var listener: LocationSource.OnLocationChangedListener? = null
    private var locationClient: AMapLocationClient? = null

    override fun activate(p0: LocationSource.OnLocationChangedListener?) {
        listener = p0
        if (locationClient == null) {
            locationClient = AMapLocationClient(context).apply {
                setLocationListener(this@AMapLocationProvider)
                val options = AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    interval = 2000
                }
                setLocationOption(options)
                startLocation()
            }
        }
    }

    override fun deactivate() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
        listener = null
    }

    override fun onLocationChanged(location: AMapLocation?) {
        if (location != null && location.errorCode == 0 && location.latitude != 0.0) {
            listener?.onLocationChanged(location)
            onLocationReady(location)
        }
    }
}

private fun setupMapHmi(aMap: AMap) {
    val myLocationStyle = MyLocationStyle().apply {
        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        showMyLocation(true)
    }
    aMap.setMyLocationStyle(myLocationStyle)
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

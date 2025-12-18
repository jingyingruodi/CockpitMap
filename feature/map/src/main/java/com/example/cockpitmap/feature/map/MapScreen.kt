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
import com.example.cockpitmap.core.model.MapController

private const val TAG = "MapRenderScreen"

/**
 * 高德地图渲染核心组件。
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
    var isGpsLocked by remember { mutableStateOf(false) }

    val ghostMarkerState = remember { mutableStateOf<Marker?>(null) }

    val mapView = remember { 
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply { onCreate(null) }
    }

    LaunchedEffect(initialLocation) {
        if (initialLocation != null && initialLocation.latitude != 0.0 && !isInitialMoveDone) {
            val aMap = mapView.map
            val latLng = LatLng(initialLocation.latitude, initialLocation.longitude)
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            
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
                            isGpsLocked = true
                            ghostMarkerState.value?.let {
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

/**
 * 地图控制器实现类。
 * [架构说明]：接口来自 :core:model，实现类封装在 :feature:map。
 */
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
    override fun setMapStyle(type: Int) {
        aMap.mapType = type
    }
}

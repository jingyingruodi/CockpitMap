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
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.example.cockpitmap.core.model.GeoLocation

/**
 * 内部调试标签
 */
private const val TAG = "MapRenderScreen"

/**
 * [MapRenderScreen]
 * 
 * 地图功能模块的顶级 Composable 组件。
 * 
 * 设计守则 (按照 MODULES.md):
 * 1. **隔离性**: 封装高德 SDK 内部细节，对外仅通过 [MapController] 接口通信。
 * 2. **健壮性**: 处理了隐私合规启动、首次定位 (0,0) 过滤、以及加载状态反馈。
 * 3. **资源管理**: 严格绑定 Activity 生命周期，确保在车机熄屏或后台时停止定位以节省功耗。
 * 
 * @param modifier 布局修饰符
 * @param initialLocation 默认展示的地理位置（当 GPS 尚未锁定时的兜底方案）
 * @param onControllerReady 当地图初始化完成并可以被外部控制时的回调
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null,
    onControllerReady: (MapController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    // 状态：控制加载环的显示/隐藏
    var isMapVisible by remember { mutableStateOf(false) }
    
    // 状态：确保自动居中逻辑在应用生命周期内仅触发一次
    var hasAutoCentered by remember { mutableStateOf(false) }

    // 【架构关键】：隐私协议必须同步、前置执行。
    // 在 remember 中执行确保它在 MapView 构造前完成。
    val mapView = remember { 
        Log.d(TAG, "Init: 执行高德隐私合规检查")
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        
        Log.d(TAG, "Init: 创建 MapView 实例")
        MapView(context).apply {
            // AMap 渲染引擎激活
            onCreate(null) 
        }
    }

    // 生命周期联动：防止车机端内存泄漏
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
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

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // AndroidView 桥接原生 MapView
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val aMap = view.map
                if (!isMapVisible) {
                    setupAMapHmi(aMap)
                    
                    // 定位回调监听：处理首次居中逻辑
                    aMap.setOnMyLocationChangeListener { location ->
                        // 过滤无效的 (0,0) 坐标，防止镜头“入海”
                        if (location != null && location.latitude != 0.0) {
                            if (!hasAutoCentered) {
                                Log.i(TAG, "定位 Fix: 捕获到真实坐标, 开始首次居中")
                                aMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(location.latitude, location.longitude), 15f
                                    )
                                )
                                hasAutoCentered = true
                            }
                        }
                    }

                    // 初始视角设定
                    initialLocation?.let {
                        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 10f))
                    }

                    // 暴露控制接口
                    onControllerReady(AMapController(aMap))
                    isMapVisible = true
                }
            }
        )

        // 加载视觉反馈：提升车机端的用户体验
        if (!isMapVisible) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Cyan
            )
        }
    }
}

/**
 * [setupAMapHmi]
 * 
 * 针对车机人机交互 (HMI) 优化的地图属性配置。
 * 1. 禁用所有 SDK 自带的 UI 元素，实现视觉全自主化。
 * 2. 强制开启夜间模式，符合智能座舱视觉审美。
 */
private fun setupAMapHmi(aMap: AMap) {
    aMap.apply {
        val myLocationStyle = MyLocationStyle().apply {
            // 模式：连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。
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
            isScaleControlsEnabled = true
        }
        mapType = AMap.MAP_TYPE_NIGHT
    }
}

/**
 * [MapController]
 * 
 * 跨模块调用的地图控制协议。所有 feature 模块（如语音、导航）
 * 只能通过此接口与地图交互，严禁直接引用 AMap 实例。
 */
interface MapController {
    /** 镜头平滑放大 */
    fun zoomIn()
    /** 镜头平滑缩小 */
    fun zoomOut()
    /** 移动镜头到指定点 */
    fun moveTo(location: GeoLocation)
    /** 手动触发定位居中 */
    fun locateMe()
}

/**
 * [AMapController]
 * 
 * [MapController] 接口在高德 SDK 下的具体实现。
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
        } else {
            Log.e(TAG, "Action: 定位居中失败，当前无有效位置信息")
        }
    }
}

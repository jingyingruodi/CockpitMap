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

private const val TAG = "MapRenderScreen"

/**
 * 高德地图渲染核心组件
 * 
 * 按照 [MODULES.md] 规范：
 * 1. 内部处理高德 SDK 的复杂定位监听逻辑，隔离第三方 SDK 细节。
 * 2. 自动处理“首次定位居中”与“定位异常超时”。
 * 3. [onControllerReady] 用于向宿主模块（如 app）暴露控制句柄。
 */
@Composable
fun MapRenderScreen(
    modifier: Modifier = Modifier,
    initialLocation: GeoLocation? = null,
    onControllerReady: (MapController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    // 状态：地图 UI 是否已完成初次渲染
    var isMapVisible by remember { mutableStateOf(false) }
    // 状态：是否已完成首次定位自动居中（防止重复拉回镜头打断用户操作）
    var hasAutoCentered by remember { mutableStateOf(false) }

    // 1. 初始化隐私协议 (必须在 MapView 创建前调用，否则会导致黑屏或 SDK 拦截)
    remember {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        true
    }

    // 2. 持久化 MapView，确保 Compose 重组时不会销毁地图底层 View
    val mapView = remember { 
        MapView(context).apply { onCreate(null) }
    }

    // 3. 将高德地图的 Native 生命周期与 Compose 生命周期严格绑定
    // 这是防止车机长时间运行内存泄漏的关键配置
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
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val aMap = view.map
                if (!isMapVisible) {
                    setupAMap(aMap)
                    
                    // 注册定位变化监听
                    aMap.setOnMyLocationChangeListener { location ->
                        // 【关键修复】：校验坐标是否有效。防止获取到 (0,0) 导致镜头飞到大西洋
                        if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                            if (!hasAutoCentered) {
                                Log.d(TAG, "获取到真实位置: ${location.latitude}, ${location.longitude}")
                                aMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(location.latitude, location.longitude), 15f
                                    )
                                )
                                hasAutoCentered = true
                            }
                        } else {
                            Log.w(TAG, "接收到无效定位数据 (0,0)，忽略移动请求")
                        }
                    }

                    // 兜底逻辑：如果 GPS 定位较慢，先展示传入的初始位置（避免用户看黑屏）
                    initialLocation?.let {
                        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 10f))
                    }

                    // 将控制器回调给 Activity/ViewModel
                    onControllerReady(AMapController(aMap))
                    isMapVisible = true
                }
            }
        )

        // 渲染覆盖层：在地图完全加载前显示进度条
        if (!isMapVisible) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Cyan
            )
        }
    }
}

/**
 * AMap 基础属性与车载 HMI 风格配置
 */
private fun setupAMap(aMap: AMap) {
    aMap.apply {
        // 配置定位蓝点样式
        val myLocationStyle = MyLocationStyle().apply {
            // 设置定位模式：连续定位，但镜头不自动居中（由我们自定义的 hasAutoCentered 控制）
            myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            interval(2000) // 车机场景下，2秒更新一次位置可平衡平滑度与性能
            showMyLocation(true)
        }
        
        setMyLocationStyle(myLocationStyle)
        isMyLocationEnabled = true // 激活高德内部定位图层
        
        uiSettings.apply {
            isZoomControlsEnabled = false     // 禁用高德自带的小缩放键
            isMyLocationButtonEnabled = false // 禁用高德原生的定位按钮，由宿主模块统一提供
            isCompassEnabled = true           // 显示指南针
        }
        // 车载专用：强制开启夜间高对比度模式
        mapType = AMap.MAP_TYPE_NIGHT
    }
}

/**
 * 跨模块地图控制抽象接口
 * 定义在 [:feature:map]，遵循业务逻辑内聚原则
 */
interface MapController {
    /** 放大地图视角 */
    fun zoomIn()
    /** 缩小地图视角 */
    fun zoomOut()
    /** 平滑移动到指定地理坐标 */
    fun moveTo(location: GeoLocation)
    /** 立即将地图中心对准当前自我位置 */
    fun locateMe()
}

/**
 * [MapController] 的具体实现，持有 AMap 实例进行操作
 */
class AMapController(private val aMap: AMap) : MapController {
    override fun zoomIn() {
        aMap.animateCamera(CameraUpdateFactory.zoomIn())
    }

    override fun zoomOut() {
        aMap.animateCamera(CameraUpdateFactory.zoomOut())
    }

    override fun moveTo(location: GeoLocation) {
        aMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
    }

    override fun locateMe() {
        val location = aMap.myLocation
        // 同样在手动定位时进行 (0,0) 校验
        if (location != null && location.latitude != 0.0) {
            aMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 15f
                )
            )
        } else {
            Log.e(TAG, "手动定位失败：暂无有效位置信息")
        }
    }
}

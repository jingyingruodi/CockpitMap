package com.example.cockpitmap.feature.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cockpitmap.core.data.repository.RouteRepository
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.RouteInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 导航路径规划业务逻辑。
 * 
 * [职责描述]：
 * 1. 协调路径规划请求。
 * 2. 管理导航预览与导航进行中的状态。
 */
class RoutingViewModel(private val repository: RouteRepository) : ViewModel() {

    private val _routeResult = MutableStateFlow<RouteInfo?>(null)
    /** 当前规划出的路径信息 */
    val routeResult: StateFlow<RouteInfo?> = _routeResult.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    /** 是否正在规划路径 */
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    // [新增加固]：导航进行状态
    private val _isNavigating = MutableStateFlow(false)
    /** 是否处于正式导航模式 */
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    /**
     * 规划驾车路径。
     */
    fun planRoute(start: GeoLocation, end: GeoLocation) {
        viewModelScope.launch {
            _isCalculating.value = true
            val result = repository.calculateDriveRoute(start, end)
            _routeResult.value = result
            _isCalculating.value = false
        }
    }

    /**
     * 进入正式导航引导模式。
     */
    fun startNavigation() {
        _isNavigating.value = true
    }

    /**
     * 退出导航或清除路径。
     */
    fun stopNavigation() {
        _isNavigating.value = false
        _routeResult.value = null
    }

    fun clearRoute() {
        _routeResult.value = null
    }
}

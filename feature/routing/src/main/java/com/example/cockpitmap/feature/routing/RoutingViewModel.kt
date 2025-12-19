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
 * [职责]：
 * 1. 协调路径规划请求。
 * 2. 维护导航预览状态。
 */
class RoutingViewModel(private val repository: RouteRepository) : ViewModel() {

    private val _routeResult = MutableStateFlow<RouteInfo?>(null)
    /** 当前规划出的路径信息 */
    val routeResult: StateFlow<RouteInfo?> = _routeResult.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    /** 是否正在规划路径 */
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    /**
     * 规划驾车路径。
     * 
     * @param start 起点
     * @param end 终点
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
     * 清除当前的路径规划结果。
     */
    fun clearRoute() {
        _routeResult.value = null
    }
}

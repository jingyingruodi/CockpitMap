package com.example.cockpitmap.feature.map

import androidx.lifecycle.ViewModel
import com.example.cockpitmap.core.model.GeoLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 地图核心状态管理器。
 * 
 * [职责描述]：
 * 1. 维护当前地图的交互状态（如定位是否锁定）。
 * 2. 缓存最新的位置信息供其他 Feature 引用。
 */
class MapViewModel : ViewModel() {

    // 定位是否已锁定
    private val _isLocationLocked = MutableStateFlow(false)
    val isLocationLocked: StateFlow<Boolean> = _isLocationLocked.asStateFlow()

    // 当前最新位置
    private val _currentLocation = MutableStateFlow<GeoLocation?>(null)
    val currentLocation: StateFlow<GeoLocation?> = _currentLocation.asStateFlow()

    /**
     * 更新定位状态。
     */
    fun updateLocation(location: GeoLocation) {
        _currentLocation.value = location
        _isLocationLocked.value = true
    }

    /**
     * 强行解除定位锁定（例如用户手动拖动地图后）。
     */
    fun unlockLocation() {
        _isLocationLocked.value = false
    }
}

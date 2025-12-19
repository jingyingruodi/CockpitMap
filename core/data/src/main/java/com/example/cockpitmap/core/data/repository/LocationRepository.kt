package com.example.cockpitmap.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cockpitmap.core.model.GeoLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 全局唯一的 DataStore 委托，防止多实例冲突
private val Context.locationDataStore by preferencesDataStore(name = "location_cache")

/**
 * 终端位置持久化仓库。
 * 
 * [职责描述]：
 * 负责设备最近一次有效地理位置的读取与存储，为应用提供启动时的“秒速回显”能力。
 */
class LocationRepository(private val context: Context) {

    private val KEY_LAT = doublePreferencesKey("last_lat")
    private val KEY_LNG = doublePreferencesKey("last_lng")
    private val KEY_NAME = stringPreferencesKey("last_name")

    /**
     * 获取最近一次成功定位的坐标流。
     * 启动时发出缓存点，若无缓存则发出 null。
     */
    val lastKnownLocation: Flow<GeoLocation?> = context.locationDataStore.data.map { prefs ->
        val lat = prefs[KEY_LAT] ?: 0.0
        val lng = prefs[KEY_LNG] ?: 0.0
        if (lat != 0.0 && lng != 0.0) {
            GeoLocation(lat, lng, prefs[KEY_NAME])
        } else {
            null
        }
    }

    /**
     * 持久化当前位置坐标。
     * 
     * @param location 最新的有效坐标领域模型。
     */
    suspend fun saveLastLocation(location: GeoLocation) {
        context.locationDataStore.edit { prefs ->
            prefs[KEY_LAT] = location.latitude
            prefs[KEY_LNG] = location.longitude
            location.name?.let { prefs[KEY_NAME] = it }
        }
    }
}

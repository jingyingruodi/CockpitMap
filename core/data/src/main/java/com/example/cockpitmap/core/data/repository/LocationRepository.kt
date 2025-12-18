package com.example.cockpitmap.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cockpitmap.core.model.GeoLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [LocationRepository]
 * 
 * 职责：
 * 1. 负责设备位置信息的持久化存储（利用 Jetpack DataStore）。
 * 2. 提供位置变化的冷响应流，用于应用启动时的“视角回显”。
 */
class LocationRepository(private val context: Context) {

    companion object {
        // Preference DataStore 扩展属性
        private val Context.dataStore by preferencesDataStore(name = "location_cache")
        
        // 存储键定义
        private val KEY_LAT = doublePreferencesKey("last_lat")
        private val KEY_LNG = doublePreferencesKey("last_lng")
        private val KEY_NAME = stringPreferencesKey("last_name")
    }

    /**
     * 获取最近一次成功定位的坐标。
     * 
     * [功能说明]：
     * 应用启动时，该流会发出 DataStore 中存储的坐标点。
     * 如果是首次安装启动，流会发出 null。
     */
    val lastKnownLocation: Flow<GeoLocation?> = context.dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAT] ?: 0.0
        val lng = prefs[KEY_LNG] ?: 0.0
        if (lat != 0.0) {
            GeoLocation(lat, lng, prefs[KEY_NAME])
        } else null
    }

    /**
     * 持久化当前位置。
     * 
     * @param location 最新的有效坐标点。
     */
    suspend fun saveLastLocation(location: GeoLocation) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAT] = location.latitude
            prefs[KEY_LNG] = location.longitude
            location.name?.let { prefs[KEY_NAME] = it }
        }
    }
}

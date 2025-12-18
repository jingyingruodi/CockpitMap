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
 * 扩展属性：创建全局单例 DataStore。
 * 按照 Jetpack 官方建议，在 Context 级别定义扩展属性以确保单例。
 */
private val Context.locationDataStore by preferencesDataStore(name = "location_settings")

/**
 * [LocationRepository]
 * 
 * 职责：
 * 1. 负责位置信息的持久化（即“上次成功位置”缓存）。
 * 2. 向上层屏蔽底层存储（DataStore）的细节。
 * 
 * 符合 [MODULES.md] 设计原则：
 * - 位于 core:data 模块，处理单一真相源（SSOT）。
 */
class LocationRepository(private val context: Context) {

    companion object {
        private val KEY_LATITUDE = doublePreferencesKey("last_latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("last_longitude")
        private val KEY_NAME = stringPreferencesKey("last_location_name")
    }

    /**
     * 获取最后一次成功的定位坐标。
     * 
     * [特性]：暴露为 Flow 冷流，UI 层可直接 collectAsState 使用。
     * 如果本地无缓存，流将发射 null。
     */
    val lastKnownLocation: Flow<GeoLocation?> = context.locationDataStore.data.map { prefs ->
        val lat = prefs[KEY_LATITUDE]
        val lng = prefs[KEY_LONGITUDE]
        if (lat != null && lng != null) {
            GeoLocation(
                latitude = lat,
                longitude = lng,
                name = prefs[KEY_NAME] ?: "上次位置"
            )
        } else {
            null
        }
    }

    /**
     * 持久化保存位置信息。
     * 
     * @param location 需要保存的地理坐标模型
     * [注意]：这是一个挂起函数，应当在 IO 协程作用域中调用。
     */
    suspend fun saveLastLocation(location: GeoLocation) {
        context.locationDataStore.edit { prefs ->
            prefs[KEY_LATITUDE] = location.latitude
            prefs[KEY_LONGITUDE] = location.longitude
            prefs[KEY_NAME] = location.name ?: "未知位置"
        }
    }
}

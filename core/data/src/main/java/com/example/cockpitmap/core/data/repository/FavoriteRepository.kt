package com.example.cockpitmap.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cockpitmap.core.model.SavedLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoriteDataStore by preferencesDataStore(name = "favorites")

/**
 * 常用地点数据仓库。
 * 
 * 使用 DataStore Preferences 存储 JSON 格式的地点列表。
 */
class FavoriteRepository(private val context: Context) {
    private val gson = Gson()
    private val FAVORITES_KEY = stringPreferencesKey("saved_locations")

    /**
     * 获取所有保存的地点流。
     */
    val savedLocations: Flow<List<SavedLocation>> = context.favoriteDataStore.data
        .map { preferences ->
            val json = preferences[FAVORITES_KEY] ?: "[]"
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            gson.fromJson(json, type)
        }

    /**
     * 保存一个新的地点。
     * 如果 ID 已存在，则覆盖。
     */
    suspend fun saveLocation(location: SavedLocation) {
        context.favoriteDataStore.edit { preferences ->
            val currentJson = preferences[FAVORITES_KEY] ?: "[]"
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            val currentList: MutableList<SavedLocation> = gson.fromJson(currentJson, type)
            
            // 移除旧的（如果 ID 相同）
            currentList.removeAll { it.id == location.id || (it.type == location.type && it.type != com.example.cockpitmap.core.model.LocationType.CUSTOM && it.type != com.example.cockpitmap.core.model.LocationType.FAVORITE) }
            currentList.add(location)
            
            preferences[FAVORITES_KEY] = gson.toJson(currentList)
        }
    }

    /**
     * 删除指定的地点。
     */
    suspend fun deleteLocation(id: String) {
        context.favoriteDataStore.edit { preferences ->
            val currentJson = preferences[FAVORITES_KEY] ?: "[]"
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            val currentList: MutableList<SavedLocation> = gson.fromJson(currentJson, type)
            
            currentList.removeAll { it.id == id }
            preferences[FAVORITES_KEY] = gson.toJson(currentList)
        }
    }
}

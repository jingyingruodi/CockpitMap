package com.example.cockpitmap.core.network

import android.content.Context
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.SearchSuggestion
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 搜索联想数据源。
 * 
 * [职责描述]：
 * 1. 封装高德地图 SDK 的 Inputtips 搜索联想 API。
 * 2. 负责将基于 Callback 的异步 API 转换为挂起函数（Coroutine-based）。
 */
class SearchDataSource(private val context: Context) {

    /**
     * 根据关键字获取搜索联想列表。
     * 
     * @param query 搜索关键字（如 "加油站"）。
     * @param city 搜索城市范围（留空则全国搜索）。
     * @return 过滤后的联想结果列表。
     */
    suspend fun getSearchSuggestions(query: String, city: String = ""): List<SearchSuggestion> = suspendCoroutine { continuation ->
        val inputTipsQuery = InputtipsQuery(query, city).apply {
            cityLimit = false // 允许跨城市搜索联想
        }
        
        val inputTips = Inputtips(context, inputTipsQuery)
        inputTips.setInputtipsListener { tipList, rCode ->
            // AMap SDK 成功码为 1000
            if (rCode == 1000 && tipList != null) {
                val suggestions = tipList
                    .filter { it.point != null } // 过滤掉没有坐标的无效点
                    .map { tip ->
                        SearchSuggestion(
                            id = tip.adcode ?: "",
                            title = tip.name ?: "未知地点",
                            snippet = tip.address ?: "",
                            location = GeoLocation(tip.point.latitude, tip.point.longitude, tip.name)
                        )
                    }
                continuation.resume(suggestions)
            } else {
                // 失败场景返回空列表
                continuation.resume(emptyList())
            }
        }
        inputTips.requestInputtipsAsyn()
    }
}

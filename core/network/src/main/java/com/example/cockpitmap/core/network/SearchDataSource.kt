package com.example.cockpitmap.core.network

import android.content.Context
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.SearchSuggestion
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 搜索数据源。
 * 
 * [职责描述]：
 * 该类属于 core:network 层，直接与高德地图搜索 SDK 进行交互。
 * 负责将高德的异步搜索回调（Callback-based）转换为现代化的 Kotlin 挂起函数（Coroutine-based）。
 */
class SearchDataSource(private val context: Context) {

    /**
     * 根据关键字获取 POI 搜索联想列表。
     * 
     * [实现细节]：
     * 利用 AMap [Inputtips] 接口。我们将异步结果封装在 [suspendCoroutine] 中，
     * 从而允许上层以同步非阻塞的方式编写业务代码。
     * 
     * @param query 用户在搜索框输入的文本。
     * @param city 搜索的城市范围限制（留空则为全国范围）。
     * @return 过滤后的 [SearchSuggestion] 列表。
     */
    suspend fun getSearchSuggestions(query: String, city: String = ""): List<SearchSuggestion> = suspendCoroutine { continuation ->
        // 构造高德查询条件
        val inputTipsQuery = InputtipsQuery(query, city).apply {
            cityLimit = false // 允许超出当前城市范围搜索
        }
        
        val inputTips = Inputtips(context, inputTipsQuery)
        inputTips.setInputtipsListener { tipList, rCode ->
            // AMap SDK 规范返回码 1000 代表操作成功
            if (rCode == 1000 && tipList != null) {
                val suggestions = tipList
                    .filter { it.point != null } // 仅保留带有经纬度的搜索结果，确保可导航
                    .map { tip ->
                        SearchSuggestion(
                            id = tip.adcode ?: "",
                            title = tip.name ?: "未知地点",
                            snippet = tip.address ?: "",
                            location = tip.point?.let { GeoLocation(it.latitude, it.longitude, tip.name) }
                        )
                    }
                continuation.resume(suggestions)
            } else {
                // 若失败（如无网络或配额超限），返回空列表，防止上层崩溃
                continuation.resume(emptyList())
            }
        }
        
        // 启动高德异步请求
        inputTips.requestInputtipsAsyn()
    }
}

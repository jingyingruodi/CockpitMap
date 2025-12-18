package com.example.cockpitmap.core.network

import android.content.Context
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.SearchSuggestion
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 搜索数据源
 * 
 * 职责：调用高德 SDK 的 Inputtips 接口获取搜索联想建议。
 */
class SearchDataSource(private val context: Context) {

    /**
     * 获取联想搜索建议。
     * 
     * @param query 搜索关键字。
     * @param city 搜索城市范围。
     */
    suspend fun getSearchSuggestions(query: String, city: String = ""): List<SearchSuggestion> = suspendCoroutine { continuation ->
        val inputTipsQuery = InputtipsQuery(query, city)
        inputTipsQuery.cityLimit = false
        
        val inputTips = Inputtips(context, inputTipsQuery)
        inputTips.setInputtipsListener { tipList, rCode ->
            if (rCode == 1000 && tipList != null) {
                val suggestions = tipList.map { tip ->
                    SearchSuggestion(
                        id = tip.adcode ?: "",
                        title = tip.name ?: "",
                        snippet = tip.address ?: "",
                        location = tip.point?.let { GeoLocation(it.latitude, it.longitude, tip.name) }
                    )
                }
                continuation.resume(suggestions)
            } else {
                continuation.resume(emptyList())
            }
        }
        inputTips.requestInputtipsAsyn()
    }
}

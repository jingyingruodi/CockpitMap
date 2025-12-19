package com.example.cockpitmap.core.network

import android.content.Context
import android.util.Log
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.example.cockpitmap.core.model.GeoLocation
import com.example.cockpitmap.core.model.SearchSuggestion
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 搜索数据源（增强调试版）。
 */
class SearchDataSource(private val context: Context) {

    suspend fun getSearchSuggestions(query: String, city: String = ""): List<SearchSuggestion> = suspendCoroutine { continuation ->
        Log.d("SearchSDK", "Starting search for: $query")
        
        val inputTipsQuery = InputtipsQuery(query, city).apply {
            cityLimit = false 
        }
        
        val inputTips = Inputtips(context, inputTipsQuery)
        inputTips.setInputtipsListener { tipList, rCode ->
            // [关键调试]：Logcat 会输出具体的返回码
            if (rCode == 1000) {
                Log.d("SearchSDK", "Success! Results size: ${tipList?.size ?: 0}")
                val suggestions = tipList?.filter { it.point != null }?.map { tip ->
                    SearchSuggestion(
                        id = tip.adcode ?: "",
                        title = tip.name ?: "未知地点",
                        snippet = tip.address ?: "",
                        location = tip.point?.let { GeoLocation(it.latitude, it.longitude, tip.name) }
                    )
                } ?: emptyList()
                continuation.resume(suggestions)
            } else {
                // 如果是 7 (Key错误) 或 12 (权限错误)，这里会一目了然
                Log.e("SearchSDK", "Error! AMap Return Code: $rCode")
                continuation.resume(emptyList())
            }
        }
        
        inputTips.requestInputtipsAsyn()
    }
}

package com.example.cockpitmap.core.data.repository

import com.example.cockpitmap.core.model.SearchSuggestion
import com.example.cockpitmap.core.network.SearchDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 搜索业务仓库。
 * 
 * [职责说明]：
 * 负责调度高德搜索数据源，并向上层暴露统一的流式联想接口。
 */
class SearchRepository(private val searchDataSource: SearchDataSource) {

    /**
     * 执行关键词地点联想搜索。
     * 
     * @param query 搜索关键字（如 "加油站"）。
     * @return 包含建议列表的 Flow。
     */
    fun searchSuggestions(query: String): Flow<List<SearchSuggestion>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        // 调用 core:network 模块封装的高德联想接口
        val results = searchDataSource.getSearchSuggestions(query)
        emit(results)
    }
}

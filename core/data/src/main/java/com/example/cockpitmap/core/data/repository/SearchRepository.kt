package com.example.cockpitmap.core.data.repository

import com.example.cockpitmap.core.model.SearchSuggestion
import com.example.cockpitmap.core.network.SearchDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 搜索业务仓库。
 * 
 * [职责描述]：
 * 负责调度搜索数据源，并向上层 (ViewModel) 暴露统一的数据接口。
 * 采用响应式 Flow 结构，确保搜索联想能够实时更新至 UI。
 */
class SearchRepository(private val searchDataSource: SearchDataSource) {

    /**
     * 执行关键词联想搜索。
     * 
     * @param query 用户输入的搜索关键字（如 "加油站"）。
     * @return 联想建议列表流。
     */
    fun searchSuggestions(query: String): Flow<List<SearchSuggestion>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        // 调用 core:network 模块封装的高德搜索接口
        val results = searchDataSource.getSearchSuggestions(query)
        emit(results)
    }
}

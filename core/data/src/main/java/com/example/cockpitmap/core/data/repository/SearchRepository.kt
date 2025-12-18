package com.example.cockpitmap.core.data.repository

import com.example.cockpitmap.core.model.SearchSuggestion
import com.example.cockpitmap.core.network.SearchDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [SearchRepository]
 * 
 * 职责：
 * 负责搜索业务逻辑的调度，连接网络数据源。
 * 
 * 符合 [MODULES.md] 规范：
 * - 位于 core:data 层。
 * - 封装了搜索算法或缓存策略。
 */
class SearchRepository(private val dataSource: SearchDataSource) {

    /**
     * 根据关键字获取搜索建议。
     * 
     * @param query 用户输入的搜索词。
     * [性能优化]：在 IO 线程池中执行。
     */
    suspend fun getSearchSuggestions(query: String): List<SearchSuggestion> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        dataSource.getSearchSuggestions(query)
    }
}

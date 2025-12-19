package com.example.cockpitmap.feature.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cockpitmap.core.data.repository.SearchRepository
import com.example.cockpitmap.core.model.SearchSuggestion
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 搜索业务逻辑控制器（增强版）。
 * 
 * [优化点]：
 * 1. 增加 isSearching 状态位，为 UI 提供加载反馈。
 * 2. 优化联想阈值：至少输入 2 个字符才发起网络请求，避免无效流量。
 */
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索状态标记，用于驱动 UI 转圈提示
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    @OptIn(FlowPreview::class)
    val suggestions: StateFlow<List<SearchSuggestion>> = _searchQuery
        .debounce(500) // 500ms 防抖，对齐车载系统流畅度
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length < 2) {
                _isSearching.value = false
                flowOf(emptyList())
            } else {
                _isSearching.value = true
                repository.searchSuggestions(query)
                    .onCompletion { _isSearching.value = false }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
    }
}

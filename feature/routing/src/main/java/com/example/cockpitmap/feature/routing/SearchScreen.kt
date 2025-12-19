package com.example.cockpitmap.feature.routing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.SearchSuggestion

/**
 * 车载地点搜索界面（瘦身精修版）。
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .width(300.dp) // 瘦身：固定宽度，避免遮挡过多地图
            .padding(12.dp) // 瘦身：减小内边距
    ) {
        CockpitSurface {
            TextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索...", fontSize = 14.sp) }, // 瘦身：缩小占位符字体
                leadingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            CockpitSurface(modifier = Modifier.heightIn(max = 300.dp)) { // 瘦身：限制高度
                LazyColumn {
                    items(suggestions) { suggestion ->
                        SuggestionItem(suggestion, onClick = {
                            focusManager.clearFocus()
                            onSuggestionClick(suggestion)
                        })
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(suggestion: SearchSuggestion, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp) // 瘦身：减小间距
    ) {
        Text(
            text = suggestion.title, 
            style = MaterialTheme.typography.bodyMedium, 
            color = Color.White,
            maxLines = 1
        )
        if (suggestion.snippet.isNotEmpty()) {
            Text(
                text = suggestion.snippet,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

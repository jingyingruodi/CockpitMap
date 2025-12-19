package com.example.cockpitmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import com.example.cockpitmap.core.common.CockpitPermissionRequester
import com.example.cockpitmap.core.common.PermissionManager
import com.example.cockpitmap.core.data.repository.FavoriteRepository
import com.example.cockpitmap.core.data.repository.LocationRepository
import com.example.cockpitmap.core.data.repository.SearchRepository
import com.example.cockpitmap.core.designsystem.CockpitFloatingButton
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.*
import com.example.cockpitmap.core.network.SearchDataSource
import com.example.cockpitmap.feature.map.MapRenderScreen
import com.example.cockpitmap.feature.map.MapViewModel
import com.example.cockpitmap.feature.routing.SearchScreen
import com.example.cockpitmap.feature.routing.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 应用程序主 Activity。
 */
class MainActivity : ComponentActivity() {
    
    private val locationRepository by lazy { LocationRepository(applicationContext) }
    private val searchRepository by lazy { SearchRepository(SearchDataSource(applicationContext)) }
    private val favoriteRepository by lazy { FavoriteRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MapsInitializer.updatePrivacyShow(applicationContext, true, true)
        MapsInitializer.updatePrivacyAgree(applicationContext, true)
        ServiceSettings.updatePrivacyShow(applicationContext, true, true)
        ServiceSettings.updatePrivacyAgree(applicationContext, true)
        
        enableEdgeToEdge()
        setContent {
            SimpleCockpitTheme {
                var permissionsGranted by remember { 
                    mutableStateOf(PermissionManager.hasAllPermissions(this)) 
                }
                
                val lastKnownLoc by locationRepository.lastKnownLocation.collectAsState(initial = null)

                if (!permissionsGranted) {
                    CockpitPermissionRequester(onAllGranted = {
                        permissionsGranted = true
                    })
                }

                if (permissionsGranted) {
                    MainScreen(
                        locRepo = locationRepository,
                        searchRepo = searchRepository,
                        favRepo = favoriteRepository,
                        cachedLocation = lastKnownLoc
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}

@Composable
fun MainScreen(
    locRepo: LocationRepository,
    searchRepo: SearchRepository,
    favRepo: FavoriteRepository,
    cachedLocation: GeoLocation?
) {
    val mapViewModel = remember { MapViewModel() }
    val searchViewModel = remember { SearchViewModel(searchRepo) }
    val scope = rememberCoroutineScope()
    
    var mapController by remember { mutableStateOf<MapController?>(null) }
    var showSaveFormByLocation by remember { mutableStateOf<GeoLocation?>(null) }
    var showSaveHint by remember { mutableStateOf(false) }
    var showStyleHint by remember { mutableStateOf(false) }
    var styleHintText by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    
    val savedLocations by favRepo.savedLocations.collectAsState(initial = emptyList())
    
    val styles = CustomMapStyle.entries.toTypedArray()
    val styleNames = listOf("标准模式", "卫星模式", "夜间模式", "导航模式")
    // 将初始索引修改为 0 (标准模式)
    var currentStyleIndex by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 地图渲染
        MapRenderScreen(
            viewModel = mapViewModel,
            modifier = Modifier.fillMaxSize(),
            initialLocation = cachedLocation,
            onControllerReady = { controller -> 
                mapController = controller
                controller.setOnMarkerLongClickListener { geo ->
                    showSaveFormByLocation = geo
                }
            }
        )

        // 2. 常用地址列表 (左侧可收起)
        FavoriteListOverlay(
            visible = showFavorites,
            locations = savedLocations,
            onItemClick = { loc ->
                mapController?.showMarker(loc.location)
                showFavorites = false
            },
            onToggle = { showFavorites = !showFavorites },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
        )

        // 3. 搜索组件
        SearchScreen(
            viewModel = searchViewModel,
            onSuggestionClick = { suggestion ->
                suggestion.location?.let { loc ->
                    mapController?.showMarker(loc)
                    searchViewModel.clearSearch()
                    scope.launch {
                        showSaveHint = true
                        delay(5000)
                        showSaveHint = false
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )

        // 4. 提示信息叠加层
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 长按保存提示
            AnimatedVisibility(visible = showSaveHint) {
                CockpitSurface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(text = "💡 长按地址标点可保存常用地址", modifier = Modifier.padding(8.dp))
                }
            }
            
            Spacer(Modifier.height(12.dp))

            // 样式切换提示
            AnimatedVisibility(visible = showStyleHint) {
                CockpitSurface {
                    Text(text = styleHintText)
                }
            }
        }

        // 5. 保存表单弹窗
        if (showSaveFormByLocation != null) {
            SaveLocationDialog(
                location = showSaveFormByLocation!!,
                onDismiss = { showSaveFormByLocation = null },
                onSave = { savedLoc ->
                    scope.launch {
                        favRepo.saveLocation(savedLoc)
                        showSaveFormByLocation = null
                    }
                }
            )
        }

        // 6. 右侧快捷操作
        QuickActions(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
            onZoomIn = { mapController?.zoomIn() },
            onZoomOut = { mapController?.zoomOut() },
            onLocateMe = { mapController?.locateMe() },
            onSwitchStyle = {
                currentStyleIndex = (currentStyleIndex + 1) % styles.size
                val newStyle = styles[currentStyleIndex]
                mapController?.setMapStyle(newStyle.type)
                
                styleHintText = "视图：${styleNames[currentStyleIndex]}"
                scope.launch {
                    showStyleHint = true
                    delay(2000)
                    showStyleHint = false
                }
            }
        )
    }
}

@Composable
fun FavoriteListOverlay(
    visible: Boolean,
    locations: List<SavedLocation>,
    onItemClick: (SavedLocation) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        CockpitFloatingButton(
            onClick = onToggle,
            icon = { Icon(if (visible) Icons.Default.Close else Icons.Default.Star, contentDescription = null) }
        )
        
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = visible, enter = slideInHorizontally(), exit = slideOutHorizontally()) {
            CockpitSurface(
                modifier = Modifier.width(280.dp).heightIn(max = 400.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (locations.isEmpty()) {
                    Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("暂无常用地址", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        items(locations) { loc ->
                            ListItem(
                                headlineContent = { Text(loc.name) },
                                supportingContent = { Text(loc.type.name, style = MaterialTheme.typography.labelSmall) },
                                leadingContent = {
                                    Icon(
                                        when(loc.type) {
                                            LocationType.HOME -> Icons.Default.Home
                                            LocationType.OFFICE -> Icons.Default.Business
                                            else -> Icons.Default.Place
                                        },
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable { onItemClick(loc) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveLocationDialog(
    location: GeoLocation,
    onDismiss: () -> Unit,
    onSave: (SavedLocation) -> Unit
) {
    var selectedType by remember { mutableStateOf(LocationType.FAVORITE) }
    var customName by remember { mutableStateOf(location.name ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        CockpitSurface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).width(320.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "保存常用地址", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                
                Text(text = location.name ?: "未知地点", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TypeItem(Icons.Default.Home, "家", selectedType == LocationType.HOME) { selectedType = LocationType.HOME }
                    TypeItem(Icons.Default.Business, "公司", selectedType == LocationType.OFFICE) { selectedType = LocationType.OFFICE }
                    TypeItem(Icons.Default.Star, "收藏", selectedType == LocationType.FAVORITE) { selectedType = LocationType.FAVORITE }
                    TypeItem(Icons.Default.Edit, "自定义", selectedType == LocationType.CUSTOM) { selectedType = LocationType.CUSTOM }
                }

                if (selectedType == LocationType.CUSTOM) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("地点名称") },
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        val finalName = if (selectedType == LocationType.CUSTOM) customName else {
                            when(selectedType) {
                                LocationType.HOME -> "家"
                                LocationType.OFFICE -> "公司"
                                else -> location.name ?: "收藏地点"
                            }
                        }
                        onSave(SavedLocation(UUID.randomUUID().toString(), finalName, selectedType, location))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
fun TypeItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(32.dp)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
    }
}

@Composable
fun QuickActions(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocateMe: () -> Unit,
    onSwitchStyle: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CockpitFloatingButton(onClick = onZoomIn, icon = { Text("+") })
        Spacer(Modifier.height(12.dp))
        CockpitFloatingButton(onClick = onZoomOut, icon = { Text("-") })
        Spacer(Modifier.height(12.dp))
        
        CockpitFloatingButton(
            onClick = onSwitchStyle,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            icon = { Icon(Icons.Default.Layers, contentDescription = "样式") }
        )
        
        Spacer(Modifier.height(12.dp))
        CockpitFloatingButton(onClick = onLocateMe, icon = { Icon(Icons.Default.MyLocation, contentDescription = null) })
    }
}

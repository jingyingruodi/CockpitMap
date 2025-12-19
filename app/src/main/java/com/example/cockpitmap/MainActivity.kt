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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import com.example.cockpitmap.core.common.CockpitPermissionRequester
import com.example.cockpitmap.core.common.PermissionManager
import com.example.cockpitmap.core.data.repository.FavoriteRepository
import com.example.cockpitmap.core.data.repository.LocationRepository
import com.example.cockpitmap.core.data.repository.RouteRepository
import com.example.cockpitmap.core.data.repository.SearchRepository
import com.example.cockpitmap.core.designsystem.*
import com.example.cockpitmap.core.model.*
import com.example.cockpitmap.core.network.SearchDataSource
import com.example.cockpitmap.feature.map.MapRenderScreen
import com.example.cockpitmap.feature.map.MapViewModel
import com.example.cockpitmap.feature.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 应用程序主 Activity。
 * 
 * [职责描述]：
 * 1. 初始化系统环境与 SDK。
 * 2. 协调各个 Feature 模块的交互（如搜索、收藏、导航）。
 * 3. 管理全局 HMI 状态流转。
 */
class MainActivity : ComponentActivity() {
    
    private val locationRepository by lazy { LocationRepository(applicationContext) }
    private val searchRepository by lazy { SearchRepository(SearchDataSource(applicationContext)) }
    private val favoriteRepository by lazy { FavoriteRepository(applicationContext) }
    private val routeRepository by lazy { RouteRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // SDK 隐私协议强制初始化
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
                        searchRepo = searchRepository,
                        favRepo = favoriteRepository,
                        routeRepo = routeRepository,
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

/**
 * 车机主屏幕容器。
 * 
 * [职责描述]：
 * 组合地图、搜索、收藏及导航控制层，处理不同 HMI 状态下的组件可见性。
 */
@Composable
fun MainScreen(
    searchRepo: SearchRepository,
    favRepo: FavoriteRepository,
    routeRepo: RouteRepository,
    cachedLocation: GeoLocation?
) {
    val mapViewModel = remember { MapViewModel() }
    val searchViewModel = remember { SearchViewModel(searchRepo) }
    val routingViewModel = remember { RoutingViewModel(routeRepo) }
    val scope = rememberCoroutineScope()
    
    var mapController by remember { mutableStateOf<MapController?>(null) }
    var showSaveFormByLocation by remember { mutableStateOf<GeoLocation?>(null) }
    var showUnfavoriteConfirmByLocation by remember { mutableStateOf<SavedLocation?>(null) }
    
    var showStyleHint by remember { mutableStateOf(false) }
    var styleHintText by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    
    var pendingNavigationLocation by remember { mutableStateOf<GeoLocation?>(null) }
    
    val savedLocations by favRepo.savedLocations.collectAsState(initial = emptyList())
    val routeInfo by routingViewModel.routeResult.collectAsState()
    val isCalculating by routingViewModel.isCalculating.collectAsState()
    val isNavigating by routingViewModel.isNavigating.collectAsState()
    val currentLoc by mapViewModel.currentLocation.collectAsState()
    
    val styles = CustomMapStyle.entries.toTypedArray()
    val styleNames = listOf("标准模式", "卫星模式", "夜间模式", "导航模式")
    var currentStyleIndex by remember { mutableIntStateOf(0) }

    // 智能感应当前位置是否已在收藏夹中
    val currentSavedLoc = remember(pendingNavigationLocation, savedLocations) {
        savedLocations.find { 
            it.location.latitude == pendingNavigationLocation?.latitude && 
            it.location.longitude == pendingNavigationLocation?.longitude 
        }
    }
    val isFavorited = currentSavedLoc != null

    // 路径绘制联动
    LaunchedEffect(routeInfo) {
        if (routeInfo != null) {
            mapController?.drawRoute(routeInfo!!)
        } else {
            mapController?.clearRoute()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 底层地图渲染
        MapRenderScreen(
            viewModel = mapViewModel,
            modifier = Modifier.fillMaxSize(),
            initialLocation = cachedLocation,
            onControllerReady = { controller -> 
                mapController = controller
            }
        )

        // 2. 正式导航模式下的专用面板
        if (isNavigating) {
            NavigationDashboard(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
                onExit = {
                    routingViewModel.stopNavigation()
                    mapController?.setFollowingMode(false)
                    mapController?.clearRoute()
                    pendingNavigationLocation = null
                }
            )
        }

        // 3. 基础探索模式下的 UI (搜索、收藏)
        if (!isNavigating) {
            FavoriteListOverlay(
                visible = showFavorites,
                locations = savedLocations,
                onItemClick = { loc ->
                    mapController?.showMarker(loc.location)
                    showFavorites = false
                    pendingNavigationLocation = loc.location
                    routingViewModel.clearRoute()
                },
                onToggle = { showFavorites = !showFavorites },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            )

            SearchScreen(
                viewModel = searchViewModel,
                onSuggestionClick = { suggestion ->
                    suggestion.location?.let { dest ->
                        mapController?.showMarker(dest)
                        searchViewModel.clearSearch()
                        pendingNavigationLocation = dest
                        routingViewModel.clearRoute()
                    }
                },
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
            )
        }

        // 4. 底部动态确认与预览卡片
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            // 前往目的地确认
            if (!isNavigating && pendingNavigationLocation != null && routeInfo == null && !isCalculating) {
                GoToConfirmationCard(
                    location = pendingNavigationLocation!!,
                    isFavorited = isFavorited,
                    onConfirm = {
                        currentLoc?.let { start ->
                            routingViewModel.planRoute(start, pendingNavigationLocation!!)
                        }
                    },
                    onSave = {
                        if (isFavorited) {
                            showUnfavoriteConfirmByLocation = currentSavedLoc
                        } else {
                            showSaveFormByLocation = pendingNavigationLocation
                        }
                    },
                    onCancel = {
                        pendingNavigationLocation = null
                        mapController?.clearMarkers()
                    }
                )
            }

            // 算路 Loading 状态
            CockpitLoadingHint(
                visible = isCalculating,
                text = "正在规划最佳路线..."
            )

            // 导航路线预览卡片
            if (!isNavigating && routeInfo != null && !isCalculating) {
                RoutePreviewCard(
                    routeInfo = routeInfo!!,
                    onStartNavigation = {
                        routingViewModel.startNavigation()
                        mapController?.setFollowingMode(true)
                    },
                    onCancel = { 
                        routingViewModel.clearRoute()
                        pendingNavigationLocation = null
                    }
                )
            }
        }

        // 5. 各种业务对话框
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

        if (showUnfavoriteConfirmByLocation != null) {
            UnfavoriteConfirmDialog(
                locationName = showUnfavoriteConfirmByLocation!!.name,
                onDismiss = { showUnfavoriteConfirmByLocation = null },
                onConfirm = {
                    scope.launch {
                        favRepo.deleteLocation(showUnfavoriteConfirmByLocation!!.id)
                        showUnfavoriteConfirmByLocation = null
                    }
                }
            )
        }

        // 6. 全局提示层
        if (!isNavigating) {
            CockpitHintLayer(
                visible = showStyleHint,
                text = styleHintText,
                modifier = Modifier.padding(top = 60.dp)
            )
        }
        
        // 7. 快捷操作工具栏
        QuickActions(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            onZoomIn = { mapController?.zoomIn() },
            onZoomOut = { mapController?.zoomOut() },
            onLocateMe = { mapController?.locateMe() },
            onSwitchStyle = {
                currentStyleIndex = (currentStyleIndex + 1) % styles.size
                mapController?.setMapStyle(styles[currentStyleIndex].type)
                styleHintText = "视图：${styleNames[currentStyleIndex]}"
                scope.launch { showStyleHint = true; delay(2000); showStyleHint = false }
            }
        )
    }
}

/**
 * 取消收藏确认对话框。
 * 
 * [职责描述]：
 * 在用户点击已收藏地点的收藏按钮时弹出，防止误删常用地址。
 */
@Composable
fun UnfavoriteConfirmDialog(
    locationName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        CockpitSurface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp).width(280.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "取消收藏", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "是否从常用地址中移除 \"$locationName\"？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("移除")
                    }
                }
            }
        }
    }
}

/**
 * 常用地址悬浮列表。
 */
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
            modifier = Modifier.size(40.dp),
            icon = { Icon(if (visible) Icons.Default.Close else Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        if (visible) { Spacer(Modifier.height(4.dp)) }
        AnimatedVisibility(visible = visible, enter = slideInHorizontally(), exit = slideOutHorizontally()) {
            CockpitSurface(
                modifier = Modifier.width(200.dp).heightIn(max = 280.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (locations.isEmpty()) {
                    Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("暂无收藏", fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(2.dp)) {
                        items(locations) { loc ->
                            ListItem(
                                headlineContent = { Text(loc.name, fontSize = 13.sp) },
                                leadingContent = {
                                    Icon(
                                        when(loc.type) {
                                            LocationType.HOME -> Icons.Default.Home
                                            LocationType.OFFICE -> Icons.Default.Business
                                            else -> Icons.Default.Place
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.clickable { onItemClick(loc) }.heightIn(min = 40.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 常用地址保存表单。
 */
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
            Column(modifier = Modifier.padding(20.dp).width(280.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "保存地址", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Text(text = location.name ?: "未知地点", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TypeItem(Icons.Default.Home, "家", selectedType == LocationType.HOME) { selectedType = LocationType.HOME }
                    TypeItem(Icons.Default.Business, "公司", selectedType == LocationType.OFFICE) { selectedType = LocationType.OFFICE }
                    TypeItem(Icons.Default.Star, "收藏", selectedType == LocationType.FAVORITE) { selectedType = LocationType.FAVORITE }
                    TypeItem(Icons.Default.Edit, "自定义", selectedType == LocationType.CUSTOM) { selectedType = LocationType.CUSTOM }
                }
                if (selectedType == LocationType.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("名称", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val finalName = if (selectedType == LocationType.CUSTOM) customName else {
                            when(selectedType) {
                                LocationType.HOME -> "家"; LocationType.OFFICE -> "公司"; else -> location.name ?: "收藏地点"
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

/**
 * 收藏类型选择项。
 */
@Composable
fun TypeItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
    }
}

/**
 * 右侧快捷操作栏。
 */
@Composable
fun QuickActions(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocateMe: () -> Unit,
    onSwitchStyle: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CockpitFloatingButton(onClick = onZoomIn, modifier = Modifier.size(40.dp), icon = { Text("+") })
        Spacer(Modifier.height(6.dp))
        CockpitFloatingButton(onClick = onZoomOut, modifier = Modifier.size(40.dp), icon = { Text("-") })
        Spacer(Modifier.height(6.dp))
        CockpitFloatingButton(
            onClick = onSwitchStyle,
            modifier = Modifier.size(40.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        Spacer(Modifier.height(6.dp))
        CockpitFloatingButton(onClick = onLocateMe, modifier = Modifier.size(40.dp), icon = { Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp)) })
    }
}

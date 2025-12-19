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
 * 2. 协调各个 Feature 模块的交互。
 * 3. 管理全局导航预览状态。
 */
class MainActivity : ComponentActivity() {
    
    private val locationRepository by lazy { LocationRepository(applicationContext) }
    private val searchRepository by lazy { SearchRepository(SearchDataSource(applicationContext)) }
    private val favoriteRepository by lazy { FavoriteRepository(applicationContext) }
    private val routeRepository by lazy { RouteRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐私协议初始化
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

@Composable
fun MainScreen(
    locRepo: LocationRepository,
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
    
    // UI 提示状态
    var showStyleHint by remember { mutableStateOf(false) }
    var styleHintText by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    
    var pendingNavigationLocation by remember { mutableStateOf<GeoLocation?>(null) }
    
    val savedLocations by favRepo.savedLocations.collectAsState(initial = emptyList())
    val routeInfo by routingViewModel.routeResult.collectAsState()
    val isCalculating by routingViewModel.isCalculating.collectAsState()
    val currentLoc by mapViewModel.currentLocation.collectAsState()
    
    val styles = CustomMapStyle.entries.toTypedArray()
    val styleNames = listOf("标准模式", "卫星模式", "夜间模式", "导航模式")
    var currentStyleIndex by remember { mutableIntStateOf(0) }

    // 观察路径规划结果：仅处理画线
    LaunchedEffect(routeInfo) {
        if (routeInfo != null) {
            mapController?.drawRoute(routeInfo!!)
        } else {
            mapController?.clearRoute()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 地图层
        MapRenderScreen(
            viewModel = mapViewModel,
            modifier = Modifier.fillMaxSize(),
            initialLocation = cachedLocation,
            onControllerReady = { controller -> 
                mapController = controller
            }
        )

        // 2. 常用地址列表
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

        // 3. 搜索层
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

        // 4. 底部交互控制层
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            // 前往目的地确认
            if (pendingNavigationLocation != null && routeInfo == null && !isCalculating) {
                GoToConfirmationCard(
                    location = pendingNavigationLocation!!,
                    onConfirm = {
                        currentLoc?.let { start ->
                            routingViewModel.planRoute(start, pendingNavigationLocation!!)
                        }
                    },
                    onSave = {
                        showSaveFormByLocation = pendingNavigationLocation
                    },
                    onCancel = {
                        pendingNavigationLocation = null
                        mapController?.clearMarkers()
                    }
                )
            }

            // 加载提示 (使用 DesignSystem 统一组件)
            CockpitLoadingHint(
                visible = isCalculating,
                text = "正在规划最佳路线..."
            )

            // 导航预览
            if (routeInfo != null && !isCalculating) {
                RoutePreviewCard(
                    routeInfo = routeInfo!!,
                    onStartNavigation = { /* 后续集成正式导航系统 */ },
                    onCancel = { 
                        routingViewModel.clearRoute()
                        pendingNavigationLocation = null
                    }
                )
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

        // 6. 顶部全局提示 (使用 DesignSystem 统一组件)
        CockpitHintLayer(
            visible = showStyleHint,
            text = styleHintText,
            modifier = Modifier.padding(top = 60.dp)
        )
        
        // 7. 右侧快捷操作
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

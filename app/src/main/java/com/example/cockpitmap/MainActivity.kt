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

        // 2. 常用地址列表 (瘦身版)
        FavoriteListOverlay(
            visible = showFavorites,
            locations = savedLocations,
            onItemClick = { loc ->
                mapController?.showMarker(loc.location)
                showFavorites = false
            },
            onToggle = { showFavorites = !showFavorites },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
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
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        )

        // 4. 提示信息叠加层 (精简提示)
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = showSaveHint) {
                CockpitSurface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(text = "💡 长按标点保存", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            if (showStyleHint) { Spacer(Modifier.height(8.dp)) }

            AnimatedVisibility(visible = showStyleHint) {
                CockpitSurface {
                    Text(text = styleHintText, style = MaterialTheme.typography.labelMedium)
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
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
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
            modifier = Modifier.size(48.dp), // 瘦身按钮
            icon = { Icon(if (visible) Icons.Default.Close else Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp)) }
        )
        
        if (visible) { Spacer(Modifier.height(8.dp)) }

        AnimatedVisibility(visible = visible, enter = slideInHorizontally(), exit = slideOutHorizontally()) {
            CockpitSurface(
                modifier = Modifier.width(220.dp).heightIn(max = 320.dp), // 瘦身面板
                shape = RoundedCornerShape(16.dp)
            ) {
                if (locations.isEmpty()) {
                    Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("暂无收藏", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(4.dp)) {
                        items(locations) { loc ->
                            ListItem(
                                headlineContent = { Text(loc.name, fontSize = 14.sp) }, // 瘦身文字
                                leadingContent = {
                                    Icon(
                                        when(loc.type) {
                                            LocationType.HOME -> Icons.Default.Home
                                            LocationType.OFFICE -> Icons.Default.Business
                                            else -> Icons.Default.Place
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.clickable { onItemClick(loc) }.heightIn(min = 48.dp)
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
        CockpitFloatingButton(onClick = onZoomIn, modifier = Modifier.size(44.dp), icon = { Text("+") })
        Spacer(Modifier.height(8.dp))
        CockpitFloatingButton(onClick = onZoomOut, modifier = Modifier.size(44.dp), icon = { Text("-") })
        Spacer(Modifier.height(8.dp))
        
        CockpitFloatingButton(
            onClick = onSwitchStyle,
            modifier = Modifier.size(44.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(20.dp)) }
        )
        
        Spacer(Modifier.height(8.dp))
        CockpitFloatingButton(onClick = onLocateMe, modifier = Modifier.size(44.dp), icon = { Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp)) })
    }
}

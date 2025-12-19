package com.example.cockpitmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import com.example.cockpitmap.core.common.CockpitPermissionRequester
import com.example.cockpitmap.core.common.PermissionManager
import com.example.cockpitmap.core.data.repository.LocationRepository
import com.example.cockpitmap.core.data.repository.SearchRepository
import com.example.cockpitmap.core.designsystem.CockpitFloatingButton
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.CustomMapStyle
import com.example.cockpitmap.core.model.MapController
import com.example.cockpitmap.core.network.SearchDataSource
import com.example.cockpitmap.feature.map.MapRenderScreen
import com.example.cockpitmap.feature.map.MapViewModel
import com.example.cockpitmap.feature.routing.SearchScreen
import com.example.cockpitmap.feature.routing.SearchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 应用程序主 Activity。
 */
class MainActivity : ComponentActivity() {
    
    private val locationRepository by lazy { LocationRepository(applicationContext) }
    private val searchRepository by lazy { SearchRepository(SearchDataSource(applicationContext)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // [核心加固]：在界面加载前强制激活所有高德 SDK 隐私协议
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
                        cachedLocation = lastKnownLoc
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("系统初始化中...", color = Color.White)
                        }
                    }
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
    cachedLocation: com.example.cockpitmap.core.model.GeoLocation?
) {
    val mapViewModel = remember { MapViewModel() }
    val searchViewModel = remember { SearchViewModel(searchRepo) }
    
    var mapController by remember { mutableStateOf<MapController?>(null) }
    val scope = rememberCoroutineScope()
    
    val styles = CustomMapStyle.entries.toTypedArray()
    val styleNames = listOf("标准模式", "卫星模式", "夜间模式", "导航模式")
    var currentStyleIndex by remember { mutableIntStateOf(2) } 
    
    var showStyleHint by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        MapRenderScreen(
            viewModel = mapViewModel,
            modifier = Modifier.fillMaxSize(),
            initialLocation = cachedLocation,
            onLocationChanged = { newLoc ->
                scope.launch { locRepo.saveLastLocation(newLoc) }
            },
            onControllerReady = { controller -> mapController = controller }
        )

        SearchScreen(
            viewModel = searchViewModel,
            onSuggestionClick = { suggestion ->
                suggestion.location?.let { loc ->
                    mapController?.moveTo(loc)
                }
            },
            modifier = Modifier.align(Alignment.TopStart)
        )

        AnimatedVisibility(
            visible = showStyleHint,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 160.dp)
        ) {
            CockpitSurface {
                Text(text = hintText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        QuickActions(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
            onZoomIn = { mapController?.zoomIn() },
            onZoomOut = { mapController?.zoomOut() },
            onLocateMe = { mapController?.locateMe() },
            onSwitchStyle = {
                currentStyleIndex = (currentStyleIndex + 1) % styles.size
                val newStyle = styles[currentStyleIndex]
                mapController?.setMapStyle(newStyle.type)
                
                hintText = "样式：${styleNames[currentStyleIndex]}"
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
        CockpitFloatingButton(
            onClick = onLocateMe,
            icon = { Icon(Icons.Default.MyLocation, contentDescription = "定位") }
        )
    }
}

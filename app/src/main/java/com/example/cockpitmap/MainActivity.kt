package com.example.cockpitmap

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.cockpitmap.core.data.repository.LocationRepository
import com.example.cockpitmap.core.designsystem.CockpitFloatingButton
import com.example.cockpitmap.core.designsystem.CockpitSurface
import com.example.cockpitmap.core.model.CustomMapStyle
import com.example.cockpitmap.core.model.MapController
import com.example.cockpitmap.feature.map.MapRenderScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 应用程序主 Activity。
 */
class MainActivity : ComponentActivity() {
    
    private val locationRepository by lazy { LocationRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCockpitTheme {
                var permissionsGranted by remember { mutableStateOf(false) }
                val lastKnownLoc by locationRepository.lastKnownLocation.collectAsState(initial = null)

                PermissionRequester(onAllGranted = {
                    permissionsGranted = true
                })

                if (permissionsGranted) {
                    MainScreen(
                        repository = locationRepository,
                        cachedLocation = lastKnownLoc
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {}
                }
            }
        }
    }
}

@Composable
fun PermissionRequester(onAllGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> onAllGranted() }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        ))
    }
}

@Composable
fun SimpleCockpitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}

@Composable
fun MainScreen(
    repository: LocationRepository,
    cachedLocation: com.example.cockpitmap.core.model.GeoLocation?
) {
    var mapController by remember { mutableStateOf<MapController?>(null) }
    val scope = rememberCoroutineScope()
    
    // 严格对应 CustomMapStyle 的定义顺序
    val styles = CustomMapStyle.entries.toTypedArray()
    val styleNames = listOf("标准模式", "卫星模式", "夜间模式", "导航模式")
    var currentStyleIndex by remember { mutableIntStateOf(2) } // 默认显示 NIGHT (Index 2)
    
    var showStyleHint by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapRenderScreen(
                modifier = Modifier.fillMaxSize(),
                initialLocation = cachedLocation,
                onLocationChanged = { newLoc ->
                    scope.launch { repository.saveLastLocation(newLoc) }
                },
                onControllerReady = { controller -> mapController = controller }
            )

            AnimatedVisibility(
                visible = showStyleHint,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 160.dp)
            ) {
                CockpitSurface {
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                    
                    hintText = "已切换至：${styleNames[currentStyleIndex]}"
                    scope.launch {
                        showStyleHint = true
                        delay(2000)
                        showStyleHint = false
                    }
                }
            )
        }
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

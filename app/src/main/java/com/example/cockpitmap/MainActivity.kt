package com.example.cockpitmap

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cockpitmap.core.data.repository.LocationRepository
import com.example.cockpitmap.feature.map.MapController
import com.example.cockpitmap.feature.map.MapRenderScreen
import kotlinx.coroutines.launch

/**
 * 应用程序主Activity。
 * 
 * [修改说明]：
 * 1. 彻底清除了代码中残留的非法编辑符号。
 * 2. 优化了 MainScreen 的调用逻辑。
 */
class MainActivity : ComponentActivity() {
    
    private val locationRepository by lazy { LocationRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCockpitTheme {
                var permissionsGranted by remember { mutableStateOf(false) }
                // 观察缓存位置
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
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
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

            QuickActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                onZoomIn = { mapController?.zoomIn() },
                onZoomOut = { mapController?.zoomOut() },
                onLocateMe = { mapController?.locateMe() }
            )
        }
    }
}

@Composable
fun QuickActions(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocateMe: () -> Unit
) {
    Column(modifier = modifier) {
        FloatingActionButton(onClick = onZoomIn) { Text("+") }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = onZoomOut) { Text("-") }
        Spacer(Modifier.height(16.dp))
        FloatingActionButton(onClick = onLocateMe) {
            Icon(Icons.Default.MyLocation, contentDescription = "定位")
        }
    }
}

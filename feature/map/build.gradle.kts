plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.cockpitmap.feature.map"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
    
    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    
    // 高德地图 SDK
    // 注意：高德 3D 地图 SDK (3dmap) 通常已经内置了基础定位功能类。
    // 为了防止 "Duplicate class" 冲突，我们暂时只保留地图 SDK。
    // 如果后续需要更高级的定位功能（如后台定位），再单独引入 location 并处理冲突。
    implementation(libs.amap.maps)
}

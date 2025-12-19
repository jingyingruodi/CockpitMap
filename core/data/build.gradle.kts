plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.cockpitmap.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
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
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // JSON Serialization
    implementation("com.google.code.gson:gson:2.11.0")
    
    // 高德 Search SDK (路径规划核心依赖)
    implementation(libs.amap.search)
    
    implementation(libs.androidx.core.ktx)
}

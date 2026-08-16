plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.stride.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stride.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Single swap point for the map style URL — see TrackMapView.kt. MapLibre's public demo
        // server is fine for development but isn't meant for production traffic; this becomes a
        // self-hosted Protomaps PMTiles style URL when that lands (docs/foundation.md "Route
        // mapping"). Keeping it a BuildConfig field means that swap is a one-line change here,
        // not a hunt through UI code for a hardcoded string.
        buildConfigField("String", "MAP_STYLE_URL", "\"https://demotiles.maplibre.org/style.json\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:weather"))

    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.maplibre.android.sdk)
    implementation(libs.maplibre.android.sdk.geojson)

    // Phase 4 real audio engine — see RunAudioService/Media3VoiceCueSpeaker. ExoPlayer's own
    // AudioFocusManager (setAudioAttributes(..., handleAudioFocus = true)) is what actually
    // implements docs/foundation.md's "request transient duck focus, play a clip, release focus"
    // cycle — no hand-rolled AudioManager.requestAudioFocus code needed.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // The Compose BOM applied via :core:designsystem's `api` only reaches the main
    // `implementation` configuration — androidTestImplementation needs its own platform
    // constraint, or ui-test-junit4 (declared without a version) can't resolve.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

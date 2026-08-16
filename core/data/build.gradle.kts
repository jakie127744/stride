plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

}

android {
    namespace = "com.stride.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

}

dependencies {
    // Phase 2 scaffold — module is empty; real dependencies land with its Phase 3/4/5 implementation.
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}

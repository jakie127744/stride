plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
    // api, not implementation: repository interfaces here return entity types and Flow directly
    // (see android-data-layer skill — no domain-model layer yet, so entities ARE the public
    // contract for now), so consumers need these modules' public surface too.
    // :core:health and :core:weather join this list once their Phase 5 clients exist.
    api(project(":core:common"))
    api(project(":core:database"))

    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
}

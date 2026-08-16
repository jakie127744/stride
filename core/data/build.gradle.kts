plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.stride.core.data"
    compileSdk = 36

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
    api(project(":core:common"))
    api(project(":core:database"))

    // implementation, not api: HealthConnectRepository is an internal collaborator inside
    // RunRepositoryImpl (HR enrichment + best-effort write-out on recordRun), not part of any
    // repository interface's own return-type surface.
    implementation(project(":core:health"))

    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
}

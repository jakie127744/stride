plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.stride.core.database"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Room schema exports — checked into version control so migrations can be tested
    // against real prior-version schemas once the app has shipped its first release.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // MigrationTestHelper reads the exported schema JSON as a test asset, not off the
    // filesystem path directly — this is what makes them visible to the instrumented test.
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

dependencies {
    api(project(":core:common")) // Track/RunEnvironment appear in entity + DAO signatures

    implementation(libs.androidx.core.ktx)
    // api, not implementation: StrideDatabase's public surface (RoomDatabase, withTransaction)
    // is exactly what :core:data needs on its own compile classpath to build the repository layer.
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    // MigrationTestHelper needs a real SQLite/Android runtime, not a plain JVM unit test — see
    // src/androidTest/.../MigrationTest.kt. Runs against the schema JSON files checked into
    // schemas/, so MIGRATION_1_2 is verified against the real v1 -> v2 shape, not just re-reading
    // its own SQL back.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
}

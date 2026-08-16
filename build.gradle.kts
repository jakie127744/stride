// Root build file. Convention/plugin logic is declared here at version-catalog level only —
// actual module wiring (Compose, Hilt, Room, etc.) is applied per-module starting in Phase 3.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

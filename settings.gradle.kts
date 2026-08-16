pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "stride"

// :app — the installable application shell (navigation host, DI wiring, application class)
include(":app")

// :core:* — shared, feature-agnostic building blocks
include(":core:designsystem")   // Stride's M3 Expressive theme, color/type tokens, shared components
include(":core:common")         // dispatchers, result types, shared utilities
include(":core:database")       // Room: runs, plans, sessions, shoe profiles, cue scripts
include(":core:data")           // repositories, single source of truth over :core:database + :core:health
include(":core:health")         // Health Connect read/write wrapper
include(":core:weather")        // Open-Meteo client — outdoor session conditions for coaching + stats
include(":core:maps")           // MapLibre + Protomaps PMTiles — offline-first route basemap, no API key

// :feature:* — one module per user-facing flow, depends on :core:* only, never on another :feature:*
include(":feature:onboarding")
include(":feature:plan")        // beginner + pro plan builder, adaptive scheduler
include(":feature:run")         // active-run screen, Media3 audio engine, outdoor/treadmill toggle
include(":feature:history")     // run history, shoe mileage tracker
include(":feature:livetrack")   // Live Track safety sharing
include(":feature:insights")    // performance statistics — pace trend, training load, VO2 max, PRs

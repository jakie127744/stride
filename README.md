<img src="docs/branding/stride-logo.jpg" alt="Stride app icon" width="88">

# Stride

A 5K training app for Android that starts you from the couch and finishes with a pacing coach serious enough for your fastest club runner. One app, two athletes.

Full mission, tech-stack rationale, and feature specs: [`docs/foundation.md`](docs/foundation.md).
Build sequence and current status: [`docs/roadmap.md`](docs/roadmap.md).

## Status

🚧 Pre-alpha, but every screen in the nav graph is real and working: onboarding, a countdown-timer active run with real GPS tracking, real weather (Open-Meteo), voice cues, generic music control, a shoe log, run history, computed insights, and location-share Live Track. See [`docs/roadmap.md`](docs/roadmap.md) for exactly what's real vs. still scoped down from the full spec (no basemap tiles yet, no background execution, no persistent Live Track link). Build, lint, and unit tests all verified clean; build-verified on a physical device.

## Module structure

```
app/                    installable app shell — nav host, DI wiring
core/
  designsystem/         M3 Expressive theme, color/type tokens, shared components
  common/                dispatchers, result types, shared utilities
  database/              Room: runs, plans, sessions, shoe profiles, cue scripts
  data/                  repositories — single source of truth over database + health + weather
  health/                Health Connect read/write wrapper
  weather/               Open-Meteo client — conditions for outdoor sessions
  maps/                  MapLibre + Protomaps PMTiles — offline-first route basemap, no API key
feature/
  onboarding/            track selection, first-run flow
  plan/                  beginner + pro plan builder, adaptive scheduler
  run/                   active-run screen, Media3 audio engine, outdoor/treadmill toggle
  history/               run history, shoe mileage tracker
  livetrack/             Live Track safety sharing
  insights/              performance statistics — pace trend, training load, VO2 max, PRs
```

`:feature:*` modules depend only on `:core:*` — never on each other. Note: the real screens built so far live directly under `:app/src/.../ui` rather than in these `:feature:*` modules — the pulled-forward vertical slice (see roadmap) moved faster than the module split, and `:feature:*` is still the empty Phase 2 scaffold. Migrating each screen into its proper feature module is worth doing before this grows much further.

## Building

Requires Android Studio (Ladybug or newer) — needed for the Android SDK, not just the IDE. Open the project and let Android Studio's SDK Manager prompt run once; after that:

```bash
./gradlew assembleDebug
```

## License

All rights reserved — no license granted for reuse. (Revisit before any public/open-source release if that's ever the intent.)

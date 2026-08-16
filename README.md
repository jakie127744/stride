<img src="docs/branding/stride-logo.jpg" alt="Stride app icon" width="88">

# Stride

A 5K training app for Android that starts you from the couch and finishes with a pacing coach serious enough for your fastest club runner. One app, two athletes.

Full mission, tech-stack rationale, and feature specs: [`docs/foundation.md`](docs/foundation.md).
Build sequence and current status: [`docs/roadmap.md`](docs/roadmap.md).

## Status

🚧 Pre-alpha. Core architecture is in place (Room + repositories + Hilt DI + navigation + M3 theme) and syncs cleanly in Android Studio — feature screens are still placeholders as their phases land (see roadmap).

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

`:feature:*` modules depend only on `:core:*` — never on each other.

## Building

Requires Android Studio (Ladybug or newer) — needed for the Android SDK, not just the IDE. Open the project and let Android Studio's SDK Manager prompt run once; after that:

```bash
./gradlew assembleDebug
```

## License

All rights reserved — no license granted for reuse. (Revisit before any public/open-source release if that's ever the intent.)

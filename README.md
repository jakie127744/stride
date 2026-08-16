# Stride

A 5K training app for Android that starts you from the couch and finishes with a pacing coach serious enough for your fastest club runner. One app, two athletes.

Full mission, tech-stack rationale, and feature specs: [`docs/foundation.md`](docs/foundation.md).
Build sequence and current status: [`docs/roadmap.md`](docs/roadmap.md).

## Status

🚧 Pre-alpha. Project scaffold only — no feature code yet (see roadmap).

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

Requires JDK 17 and Android Studio (Ladybug or newer). The Gradle wrapper jar isn't committed yet — on first open, Android Studio will offer to regenerate it, or run `gradle wrapper --gradle-version 8.10` once Gradle is available locally.

```bash
./gradlew assembleDebug
```

## License

All rights reserved — no license granted for reuse. (Revisit before any public/open-source release if that's ever the intent.)
